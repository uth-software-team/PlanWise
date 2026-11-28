import { useSyncExternalStore } from 'react'

import { ApiError } from '@/lib/apiClient'
import { getToken, subscribeToken } from '@/lib/auth-token'

import { addMember as postMember, getGroup, type GroupDetail } from './api/groupsApi'

/**
 * State of the group detail page (FE-GR-01, rules B2). A response is applied only while the context it was sent
 * in is still current: same token, same open group, same epoch. The epoch changes when the page opens, when the
 * token changes and when the page closes, so a late response of an earlier page, group or account changes
 * nothing. GETs also carry a counter, so only the newest GET of the current context is applied.
 */
export type GroupDetailState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'error'; code: 'FORBIDDEN' | 'RESOURCE_NOT_FOUND' | 'OTHER'; error: unknown }
  | { status: 'ready'; group: GroupDetail }

export type GroupDetailSnapshot = {
  detail: GroupDetailState
  /** A write and the reload that follows it are in flight; further writes answer `busy`. */
  pendingWrite: boolean
}

/**
 * `stale`: the page, group or account changed while the request ran; the result was dropped and must not be shown.
 * `error`: shown by the caller; for 403/404/409 the group was reloaded first.
 */
export type WriteResult =
  { status: 'ok' } | { status: 'busy' } | { status: 'stale' } | { status: 'error'; error: unknown }

type Context = { token: string | null; groupId: number; epoch: number }

const IDLE: GroupDetailSnapshot = { detail: { status: 'idle' }, pendingWrite: false }

let openGroupId: number | null = null
let boundToken: string | null = null
let epoch = 0
let latest = 0
let snapshot: GroupDetailSnapshot = IDLE
const listeners = new Set<() => void>()
let unsubscribeToken: (() => void) | null = null

function notify(): void {
  listeners.forEach((listener) => listener())
}

function setSnapshot(next: GroupDetailSnapshot): void {
  snapshot = next
  notify()
}

function currentContext(groupId: number): Context {
  return { token: getToken(), groupId, epoch }
}

function isCurrent(ctx: Context): boolean {
  return getToken() === ctx.token && openGroupId === ctx.groupId && epoch === ctx.epoch
}

function errorCode(error: unknown): 'FORBIDDEN' | 'RESOURCE_NOT_FOUND' | 'OTHER' {
  if (
    error instanceof ApiError &&
    (error.code === 'FORBIDDEN' || error.code === 'RESOURCE_NOT_FOUND')
  ) {
    return error.code
  }
  return 'OTHER'
}

/** GET for `ctx`; applied only if `ctx` is still current and no newer GET was started meanwhile. */
async function load(ctx: Context): Promise<void> {
  const id = ++latest
  try {
    const group = await getGroup(ctx.groupId)
    if (!isCurrent(ctx) || id !== latest) return
    setSnapshot({ ...snapshot, detail: { status: 'ready', group } })
  } catch (error) {
    if (!isCurrent(ctx) || id !== latest) return
    // 403: the caller is no longer a member, so nothing of the group stays on screen.
    setSnapshot({ ...snapshot, detail: { status: 'error', code: errorCode(error), error } })
  }
}

/** New epoch: drops every in-flight result, clears the group and loads it again for the current token. */
function reset(): void {
  epoch += 1
  boundToken = getToken()
  const groupId = openGroupId
  if (groupId === null || !boundToken) {
    setSnapshot(IDLE)
    return
  }
  setSnapshot({ detail: { status: 'loading' }, pendingWrite: false })
  void load(currentContext(groupId))
}

function onTokenChange(): void {
  if (openGroupId !== null && getToken() !== boundToken) reset()
}

/** Opens `groupId` (page mount or route change). Returns the function that closes it (page unmount). */
export function openGroup(groupId: number): () => void {
  openGroupId = groupId
  if (!unsubscribeToken) unsubscribeToken = subscribeToken(onTokenChange)
  reset()
  return closeGroup
}

/** Page unmount: invalidates every in-flight request of the page; nothing it started can change state later. */
export function closeGroup(): void {
  openGroupId = null
  epoch += 1
  if (unsubscribeToken) {
    unsubscribeToken()
    unsubscribeToken = null
  }
  setSnapshot(IDLE)
}

/** Reloads the open group (retry button). Keeps the current data until the response arrives. */
export function reloadGroup(): void {
  if (openGroupId === null) return
  void load(currentContext(openGroupId))
}

/**
 * Adds a member by email (B2 steps WG, W0–W5). `pendingWrite` stays true until the reload that follows the write
 * has finished, so the next write sees the new state. A result for a context that is no longer current is dropped
 * without touching state.
 */
export async function addMember(email: string): Promise<WriteResult> {
  if (snapshot.pendingWrite) return { status: 'busy' } // WG
  const groupId = openGroupId
  if (groupId === null) return { status: 'stale' }

  const ctx = currentContext(groupId) // W0
  setSnapshot({ ...snapshot, pendingWrite: true })
  let failure: { error: unknown } | null = null
  try {
    await postMember(groupId, email)
  } catch (error) {
    failure = { error }
  }

  if (!isCurrent(ctx)) return { status: 'stale' } // W1

  const reload =
    failure === null ||
    (failure.error instanceof ApiError && [403, 404, 409].includes(failure.error.status))
  if (!reload) {
    setSnapshot({ ...snapshot, pendingWrite: false }) // W4
    return { status: 'error', error: failure?.error }
  }

  latest += 1 // W2/W3: an older GET still in flight can no longer win
  await load(ctx)
  if (!isCurrent(ctx)) return { status: 'stale' } // W5
  setSnapshot({ ...snapshot, pendingWrite: false })
  return failure === null ? { status: 'ok' } : { status: 'error', error: failure.error }
}

export function subscribeGroupDetail(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getGroupDetailSnapshot(): GroupDetailSnapshot {
  return snapshot
}

/** Number of GETs started so far (checks T14c/T14e use it to prove a dropped write started no reload). */
export function getLoadCounter(): number {
  return latest
}

export function useGroupDetail(): GroupDetailSnapshot {
  return useSyncExternalStore(subscribeGroupDetail, getGroupDetailSnapshot)
}

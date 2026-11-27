import {
  ApiError,
  apiRequest,
  getAuthToken,
  setAuthToken,
  type ConflictErrorDto,
} from '@/lib/apiClient'
import type {
  CalendarResponse,
  CommittedSession,
  ConfirmRequest,
  GenerateScheduleRequest,
  ProposalBatchResponse,
  ScheduleGridSession,
  SessionEditRequest,
} from '../types'

const COMMITTED_STATUSES: ReadonlySet<string> = new Set<CommittedSession['status']>([
  'CONFIRMED',
  'COMPLETED',
])

export const scheduleApi = {
  generateProposal(request: GenerateScheduleRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>('/schedules/generate', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  getProposal(proposalId: number): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/proposals/${proposalId}`)
  },

  discardProposal(proposalId: number, version: number): Promise<void> {
    return apiRequest<void>(`/schedules/proposals/${proposalId}?version=${version}`, {
      method: 'DELETE',
    })
  },

  editSession(sessionId: number, request: SessionEditRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/sessions/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    })
  },

  deleteSession(sessionId: number, version: number): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(
      `/schedules/sessions/${sessionId}?version=${version}`,
      {
        method: 'DELETE',
      },
    )
  },

  confirmProposal(proposalId: number, request: ConfirmRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/${proposalId}/confirm`, {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  /**
   * CONFIRMED and COMPLETED sessions in [start, end) from GET /v1/calendar. PROPOSED sessions come from the
   * active proposal instead, so any other status is dropped here.
   */
  async listCommittedSessions(startIso: string, endIso: string): Promise<CommittedSession[]> {
    const params = new URLSearchParams({ start: startIso, end: endIso })
    const calendar = await apiRequest<CalendarResponse>(`/calendar?${params.toString()}`)
    return (calendar?.sessions ?? []).flatMap((s) =>
      COMMITTED_STATUSES.has(s.status)
        ? [{ ...s, status: s.status as CommittedSession['status'] }]
        : [],
    )
  },
}

// Same-tab recovery of the PROPOSED batch after a reload (option A): only the proposal ID is kept; the batch
// and its version are always re-read from GET /v1/schedules/proposals/{id}.
const ACTIVE_PROPOSAL_KEY = 'planwise.schedule.activeProposalId'

export const activeProposalStore = {
  read(): number | null {
    try {
      const raw = window.sessionStorage.getItem(ACTIVE_PROPOSAL_KEY)
      const id = raw === null ? NaN : Number(raw)
      return Number.isSafeInteger(id) && id > 0 ? id : null
    } catch {
      return null
    }
  },

  write(proposalId: number | null): void {
    try {
      if (proposalId === null) {
        window.sessionStorage.removeItem(ACTIVE_PROPOSAL_KEY)
      } else {
        window.sessionStorage.setItem(ACTIVE_PROPOSAL_KEY, String(proposalId))
      }
    } catch {
      // Storage unavailable: recovery after reload is simply skipped.
    }
  },
}

export type CalendarState =
  | { status: 'loading' }
  | { status: 'ready'; sessions: CommittedSession[] }
  | { status: 'error'; message: string }

/** Everything the schedule page shows that comes from the server or from a schedule action. */
export interface ScheduleState {
  calendar: CalendarState
  proposal: ProposalBatchResponse | null
  proposalError: string | null
  /** A saved proposal ID is being re-read; "No sessions" must wait for it. */
  proposalRestoring: boolean
  conflicts: ConflictErrorDto[]
  pageError: string | null
  generateError: string | null
  sessionError: string | null
  isBusy: boolean
}

export const INITIAL_SCHEDULE_STATE: ScheduleState = {
  calendar: { status: 'loading' },
  proposal: null,
  proposalError: null,
  proposalRestoring: false,
  conflicts: [],
  pageError: null,
  generateError: null,
  sessionError: null,
  isBusy: false,
}

function describeError(err: unknown, fallback: string): string {
  return err instanceof ApiError ? `${err.code}: ${err.message}` : fallback
}

/**
 * Drives the schedule page without React, so its action paths can be checked on their own.
 * - Calendar and the saved proposal load independently and each is published as soon as it settles.
 * - A newer load, a proposal action or an account change makes older responses stale; stale responses
 *   never touch state or the saved proposal ID.
 * - Every action is bound to the account that started it: after a token change its late success, error
 *   and finally are all ignored.
 * - The controller remembers the token it last saw. Before any load or action it compares that with
 *   localStorage, so a token replaced or removed in another tab switches accounts first and an action is
 *   never sent with the new account's credential while the page still shows the old account.
 */
export function createScheduleController(
  publish: (state: ScheduleState) => void,
  options: { onAccountChange?: (token: string) => void } = {},
) {
  let state = INITIAL_SCHEDULE_STATE
  let account = 0
  let knownToken = getAuthToken()
  let calendarTicket = 0
  let proposalTicket = 0
  let range: { startIso: string; endIso: string } | null = null

  function set(patch: Partial<ScheduleState>): void {
    state = { ...state, ...patch }
    publish(state)
  }

  /** Another account: drop the previous account's state, saved proposal ID and everything in flight. */
  function switchAccount(token: string): void {
    knownToken = token
    account++
    calendarTicket++
    proposalTicket++
    activeProposalStore.write(null)
    state = INITIAL_SCHEDULE_STATE
    publish(state)
    options.onAccountChange?.(token)
  }

  /** True when the stored token is no longer the one this page represents (it then switches accounts). */
  function accountChangedInStorage(): boolean {
    const current = getAuthToken()
    if (current === knownToken) return false
    switchAccount(current)
    return true
  }

  /**
   * True while `owner` is still this page's account and its token is still the stored one. If another tab
   * replaced the token before its storage event was handled, this switches accounts now and loads the new
   * one (the late event then finds nothing to change), and the caller drops the response.
   */
  function isCurrent(owner: number): boolean {
    if (owner !== account) return false
    if (!accountChangedInStorage()) return true
    void load()
    return false
  }

  /** The server's answer to a proposal action; it wins over any proposal load still in flight. */
  function rememberProposal(batch: ProposalBatchResponse | null): void {
    proposalTicket++
    const active = batch?.status === 'PROPOSED' ? batch : null
    activeProposalStore.write(active?.proposalId ?? null)
    set({ proposal: active, proposalError: null, proposalRestoring: false })
  }

  function load(): Promise<void> {
    if (range === null) return Promise.resolve()
    accountChangedInStorage() // then load for whichever account is stored now
    const owner = account
    const { startIso, endIso } = range
    const calendarRun = ++calendarTicket
    const proposalRun = ++proposalTicket
    const savedId = activeProposalStore.read()
    set({ calendar: { status: 'loading' }, pageError: null, proposalRestoring: savedId !== null })

    const calendar = scheduleApi.listCommittedSessions(startIso, endIso).then(
      (sessions) => {
        if (calendarRun !== calendarTicket || !isCurrent(owner)) return
        set({ calendar: { status: 'ready', sessions } })
      },
      (err: unknown) => {
        if (calendarRun !== calendarTicket || !isCurrent(owner)) return
        set({
          calendar: {
            status: 'error',
            message: `Calendar unavailable. ${describeError(err, 'Unable to reach the server.')}`,
          },
        })
      },
    )

    // No saved ID: keep whatever proposal is already on screen.
    const proposal =
      savedId === null
        ? Promise.resolve()
        : scheduleApi.getProposal(savedId).then(
            (batch) => {
              if (proposalRun !== proposalTicket || !isCurrent(owner)) return
              if (batch?.status === 'PROPOSED') {
                set({ proposal: batch, proposalError: null, proposalRestoring: false })
              } else {
                activeProposalStore.write(null)
                set({ proposal: null, proposalError: null, proposalRestoring: false })
              }
            },
            (err: unknown) => {
              if (proposalRun !== proposalTicket || !isCurrent(owner)) return
              if (err instanceof ApiError && err.status === 404) {
                // Replaced, deleted or not owned by the current account.
                activeProposalStore.write(null)
                set({ proposal: null, proposalError: null, proposalRestoring: false })
              } else {
                // Keep the saved ID so a later reload can retry.
                set({
                  proposalRestoring: false,
                  proposalError: `Could not restore the open proposal. ${describeError(err, 'Unable to reach the server.')}`,
                })
              }
            },
          )

    return Promise.all([calendar, proposal]).then(() => undefined)
  }

  /** Runs one action for the account that started it. Resolves true only if it succeeded for that account. */
  async function act<T>(
    clear: Partial<ScheduleState>,
    request: () => Promise<T>,
    onSuccess: (value: T) => void,
    onError: (err: unknown) => Partial<ScheduleState>,
  ): Promise<boolean> {
    if (accountChangedInStorage()) {
      // The page still showed the previous account: never send its action with the new credential.
      void load()
      return false
    }
    const owner = account
    set({ ...clear, conflicts: [], isBusy: true })
    try {
      // request() reads the token synchronously, in the same tick as the check above.
      const value = await request()
      if (!isCurrent(owner)) return false
      onSuccess(value)
      return true
    } catch (err) {
      if (isCurrent(owner)) set(onError(err))
      return false
    } finally {
      // After an account switch (here or in isCurrent) the new account's busy state is left alone.
      if (owner === account) set({ isBusy: false })
    }
  }

  return {
    getState: (): ScheduleState => state,

    setRange(startIso: string, endIso: string): Promise<void> {
      range = { startIso, endIso }
      return load()
    },

    refresh: (): Promise<void> => load(),

    /**
     * Applies a pasted token. The same token keeps everything, including the saved proposal ID; a different
     * token is another account, so its predecessor's state, saved ID and in-flight work are dropped first.
     */
    applyToken(token: string): { changed: boolean; done: Promise<void> } {
      const next = token.trim()
      const changed = next !== knownToken
      if (changed) switchAccount(next)
      if (getAuthToken() !== next) setAuthToken(next)
      return { changed, done: load() }
    },

    /**
     * Call on every `storage` event (including `clear()`, whose key is null). localStorage already holds the
     * new value, so it is compared with the remembered token; on a change the new account is loaded.
     */
    syncWithStorage(): { changed: boolean; done: Promise<void> } {
      const changed = accountChangedInStorage()
      return { changed, done: changed ? load() : Promise.resolve() }
    },

    getToken: (): string => knownToken,

    dismiss(field: 'generateError' | 'sessionError'): void {
      set({ [field]: null })
    },

    generate: (request: GenerateScheduleRequest): Promise<boolean> =>
      act(
        { generateError: null },
        () => scheduleApi.generateProposal(request),
        rememberProposal,
        (err) => ({
          generateError:
            err instanceof ApiError && err.code === 'NO_AVAILABILITY_RULE'
              ? 'No active availability rules found. Please configure your weekly study/work hours first.'
              : describeError(err, 'Failed to generate schedule proposal.'),
        }),
      ),

    editSession: (
      sessionId: number,
      startAtIso: string,
      endAtIso: string,
      batchVersion: number,
    ): Promise<boolean> =>
      act(
        { sessionError: null },
        () =>
          scheduleApi.editSession(sessionId, {
            startAt: startAtIso,
            endAt: endAtIso,
            version: batchVersion,
          }),
        rememberProposal,
        (err) => {
          if (err instanceof ApiError && err.code === 'STALE_VERSION') void load()
          return { sessionError: describeError(err, 'Failed to update session.') }
        },
      ),

    deleteSession: (sessionId: number, batchVersion: number): Promise<boolean> =>
      act(
        { sessionError: null },
        () => scheduleApi.deleteSession(sessionId, batchVersion),
        rememberProposal,
        (err) => ({ sessionError: describeError(err, 'Failed to remove session.') }),
      ),

    confirm(): Promise<boolean> {
      const proposal = state.proposal
      if (proposal === null) return Promise.resolve(false)
      return act(
        { pageError: null },
        () => scheduleApi.confirmProposal(proposal.proposalId, { version: proposal.version }),
        () => {
          rememberProposal(null)
          void load()
        },
        (err) =>
          err instanceof ApiError && err.conflicts.length > 0
            ? { conflicts: err.conflicts }
            : { pageError: describeError(err, 'Failed to confirm schedule.') },
      )
    },

    discard(): Promise<boolean> {
      const proposal = state.proposal
      if (proposal === null) return Promise.resolve(false)
      return act(
        { pageError: null },
        () => scheduleApi.discardProposal(proposal.proposalId, proposal.version),
        () => rememberProposal(null),
        (err) => ({ pageError: describeError(err, 'Failed to discard proposal.') }),
      )
    },
  }
}

export interface ScheduleView {
  sessions: ScheduleGridSession[]
  /** Full-area spinner: calendar still loading and nothing else to show yet. */
  showSpinner: boolean
  /** Small note next to a grid that is still waiting for the calendar or a saved proposal. */
  pendingNote: string | null
  /**
   * "No sessions" message: only after the calendar loaded successfully, nothing is scheduled, no saved
   * proposal is still being restored and restoring it did not fail (its ID is kept for a retry).
   */
  showEmptyState: boolean
}

export function deriveScheduleView(state: ScheduleState): ScheduleView {
  const committed = state.calendar.status === 'ready' ? state.calendar.sessions : []
  const sessions: ScheduleGridSession[] = [...committed, ...(state.proposal?.sessions ?? [])]
  const loading = state.calendar.status === 'loading'
  const showSpinner = loading && sessions.length === 0
  let pendingNote: string | null = null
  if (!showSpinner && loading) pendingNote = 'Loading confirmed sessions...'
  else if (!showSpinner && state.proposalRestoring) pendingNote = 'Restoring your open proposal...'
  return {
    sessions,
    showSpinner,
    pendingNote,
    showEmptyState:
      state.calendar.status === 'ready' &&
      sessions.length === 0 &&
      !state.proposalRestoring &&
      state.proposalError === null,
  }
}

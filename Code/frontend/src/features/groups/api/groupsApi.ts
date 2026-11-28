import type { components } from '@/api/schema'
import { apiRequest } from '@/lib/apiClient'

export type GroupSummary = components['schemas']['GroupSummaryResponse']
export type GroupDetail = components['schemas']['GroupResponse']
export type GroupMember = components['schemas']['MemberResponse']
export type GroupRequest = components['schemas']['GroupRequest']
export type GroupRole = NonNullable<GroupMember['role']>

/** GET /groups: the caller's ACTIVE memberships with the caller's role, ordered by group id. */
export function listGroups(): Promise<GroupSummary[]> {
  return apiRequest<GroupSummary[]>('/groups')
}

/** POST /groups (201): the caller becomes the only LEADER. 400 INVALID_INPUT for a blank or too long name. */
export function createGroup(body: GroupRequest): Promise<GroupDetail> {
  return apiRequest<GroupDetail>('/groups', { method: 'POST', body: JSON.stringify(body) })
}

/** GET /groups/{id}: ACTIVE members only (403), 404 for a missing group. Members keep the backend order. */
export function getGroup(groupId: number): Promise<GroupDetail> {
  return apiRequest<GroupDetail>(`/groups/${groupId}`)
}

/**
 * POST /groups/{id}/members (201): LEADER only (403). Adds the account with this email as MEMBER.
 * 400 invalid email, 404 no active account with this email (or missing group), 409 already an ACTIVE member.
 */
export function addMember(groupId: number, email: string): Promise<GroupMember> {
  return apiRequest<GroupMember>(`/groups/${groupId}/members`, {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

/** True when `userId` is the group's ACTIVE LEADER according to `members` (D16: at most one). */
export function isLeaderOf(group: GroupDetail | null, userId: number | undefined): boolean {
  if (!group || userId === undefined) return false
  return (group.members ?? []).some(
    (member) => member.userId === userId && member.role === 'LEADER' && member.status === 'ACTIVE',
  )
}

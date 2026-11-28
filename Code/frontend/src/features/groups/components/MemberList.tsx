import type { ReactNode } from 'react'

import type { GroupMember } from '../api/groupsApi'
import { memberName, roleLabel } from '../labels'
import { Initials } from './Initials'

type MemberListProps = {
  members: GroupMember[]
  /** Id of the list heading (aria-labelledby; focus target when a removed control disappears, FE-GR-01b). */
  headingId: string
  /** Extra controls per member (LEADER actions in FE-GR-01b); nothing is rendered when it returns null. */
  renderActions?: (member: GroupMember, name: string) => ReactNode
}

/** ACTIVE members in the backend order (joinedAt, then membership id; contract O8). Identity and role only. */
export function MemberList({ members, headingId, renderActions }: MemberListProps) {
  return (
    <ul aria-labelledby={headingId} className="divide-y divide-border">
      {members.map((member) => {
        const name = memberName(member)
        return (
          <li key={member.userId} className="flex flex-wrap items-center gap-3 py-3">
            <Initials name={name} className="size-10 rounded-full text-sm" />
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium text-foreground">{name}</span>
              {member.email && member.email !== name && (
                <span className="block truncate text-sm text-muted-foreground">{member.email}</span>
              )}
            </span>
            <span
              className={
                member.role === 'LEADER'
                  ? 'rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary'
                  : 'rounded-full bg-muted px-2.5 py-0.5 text-xs font-semibold text-muted-foreground'
              }
            >
              {roleLabel(member.role)}
            </span>
            {renderActions?.(member, name)}
          </li>
        )
      })}
    </ul>
  )
}

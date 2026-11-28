/** Vietnamese label of a group role (team design: "Trưởng nhóm" / "Thành viên"). */
export const ROLE_LABEL: Record<string, string> = { LEADER: 'Trưởng nhóm', MEMBER: 'Thành viên' }

export function roleLabel(role: string | undefined): string {
  return ROLE_LABEL[role ?? ''] ?? role ?? ''
}

/** Up to two initials of a name ("Đồ án Công nghệ" → "ĐC"); "?" when there is no name. */
export function initialsOf(name: string | undefined): string {
  const words = (name ?? '').trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return '?'
  const letters = words.length === 1 ? [words[0]] : [words[0], words[words.length - 1]]
  return letters.map((word) => Array.from(word)[0].toUpperCase()).join('')
}

import { cn } from '@/lib/utils'

import { initialsOf } from '../labels'

/** Decorative initials tile (team design); the name next to it carries the meaning. */
export function Initials({ name, className }: { name: string | undefined; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-xl bg-primary/10 font-semibold text-primary',
        className,
      )}
    >
      {initialsOf(name)}
    </span>
  )
}

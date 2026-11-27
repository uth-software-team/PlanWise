import { CalendarDays } from 'lucide-react'

import { cn } from '@/lib/utils'

type BrandMarkProps = {
  inverted?: boolean
}

export function BrandMark({ inverted = false }: BrandMarkProps) {
  return (
    <span className="flex items-center gap-2.5">
      <span
        aria-hidden="true"
        className={cn(
          'flex size-9 items-center justify-center rounded-lg',
          inverted ? 'bg-white/15 text-white' : 'bg-primary text-primary-foreground',
        )}
      >
        <CalendarDays className="size-5" />
      </span>
      <span
        className={cn(
          'text-lg font-semibold tracking-tight',
          inverted ? 'text-white' : 'text-foreground',
        )}
      >
        PlanWise AI
      </span>
    </span>
  )
}

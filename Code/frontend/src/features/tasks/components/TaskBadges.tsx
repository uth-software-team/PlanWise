import { AlertTriangle } from 'lucide-react'

import { Badge, type BadgeProps } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import {
  CATEGORY_LABELS,
  PRIORITY_LABELS,
  STATUS_LABELS,
  type TaskCategory,
  type TaskPriority,
  type TaskStatus,
} from '../types'

const PRIORITY_BADGE_CONFIG: Record<
  TaskPriority,
  { variant: NonNullable<BadgeProps['variant']>; className?: string }
> = {
  LOW: { variant: 'neutral' },
  MEDIUM: { variant: 'neutral', className: 'border-sky-300 bg-sky-50 text-sky-900' },
  HIGH: { variant: 'proposed' },
  URGENT: { variant: 'danger' },
}

const STATUS_BADGE_CONFIG: Record<
  TaskStatus,
  { variant: NonNullable<BadgeProps['variant']>; className?: string }
> = {
  TODO: { variant: 'neutral' },
  IN_PROGRESS: { variant: 'neutral', className: 'border-indigo-300 bg-indigo-50 text-indigo-900' },
  COMPLETED: { variant: 'confirmed' },
  CANCELLED: { variant: 'neutral', className: 'border-zinc-300 bg-zinc-100 text-zinc-700' },
}

export function PriorityBadge({ priority }: { priority: TaskPriority }) {
  const cfg = PRIORITY_BADGE_CONFIG[priority]
  return (
    <Badge variant={cfg.variant} className={cn(cfg.className)}>
      {PRIORITY_LABELS[priority]}
    </Badge>
  )
}

export function StatusBadge({ status }: { status: TaskStatus }) {
  const cfg = STATUS_BADGE_CONFIG[status]
  return (
    <Badge variant={cfg.variant} className={cn(cfg.className)}>
      {STATUS_LABELS[status]}
    </Badge>
  )
}

export function CategoryBadge({ category }: { category: TaskCategory }) {
  return (
    <Badge variant="neutral" className="font-medium">
      {CATEGORY_LABELS[category]}
    </Badge>
  )
}

export function OverdueBadge({ overdue }: { overdue: boolean }) {
  if (!overdue) return null
  return (
    <Badge variant="danger">
      <AlertTriangle className="h-3.5 w-3.5" aria-hidden="true" />
      Quá hạn
    </Badge>
  )
}

export type RecurrenceType = 'ONCE' | 'WEEKLY'

export const DAY_OF_WEEK_OPTIONS: ReadonlyArray<{
  value: number
  label: string
  shortLabel: string
}> = [
  { value: 1, label: 'Thứ Hai', shortLabel: 'T2' },
  { value: 2, label: 'Thứ Ba', shortLabel: 'T3' },
  { value: 3, label: 'Thứ Tư', shortLabel: 'T4' },
  { value: 4, label: 'Thứ Năm', shortLabel: 'T5' },
  { value: 5, label: 'Thứ Sáu', shortLabel: 'T6' },
  { value: 6, label: 'Thứ Bảy', shortLabel: 'T7' },
  { value: 7, label: 'Chủ Nhật', shortLabel: 'CN' },
]

export function getDayOfWeekLabel(dayOfWeek: number): string {
  return DAY_OF_WEEK_OPTIONS.find((item) => item.value === dayOfWeek)?.label ?? `Ngày ${dayOfWeek}`
}

export function getDayOfWeekShortLabel(dayOfWeek: number): string {
  return (
    DAY_OF_WEEK_OPTIONS.find((item) => item.value === dayOfWeek)?.shortLabel ?? `T${dayOfWeek + 1}`
  )
}

export const RECURRENCE_LABELS: Record<RecurrenceType, string> = {
  WEEKLY: 'Hàng tuần',
  ONCE: 'Một lần',
}

export interface AvailabilityRuleResponse {
  id: number
  dayOfWeek: number
  startTime: string
  endTime: string
  active: boolean
  createdAt: string
}

export interface AvailabilityRuleRequest {
  dayOfWeek: number
  startTime: string
  endTime: string
  active?: boolean
}

export interface FixedEventResponse {
  id: number
  title: string
  note: string | null
  recurrenceType: RecurrenceType
  dayOfWeek: number | null
  startTime: string | null
  endTime: string | null
  startAt: string | null
  endAt: string | null
  effectiveFrom: string | null
  effectiveUntil: string | null
  warnings: string[]
  createdAt: string
  updatedAt: string
}

export interface FixedEventRequest {
  title: string
  note: string | null
  recurrenceType: RecurrenceType
  dayOfWeek: number | null
  startTime: string | null
  endTime: string | null
  startAt: string | null
  endAt: string | null
  effectiveFrom: string | null
  effectiveUntil: string | null
}

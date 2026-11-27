export { cn, type ClassValue } from 'cn'

export function toLocalDateTimeInput(isoString: string): string {
  const d = new Date(isoString)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function fromLocalDateTimeInput(localValue: string): string {
  return new Date(localValue).toISOString()
}

export function formatTimeRange(startIso: string, endIso: string): string {
  const s = new Date(startIso)
  const e = new Date(endIso)
  const fmt: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', hour12: false }
  return `${s.toLocaleTimeString([], fmt)} - ${e.toLocaleTimeString([], fmt)}`
}

import { useState, type FormEvent } from 'react'
import { Sparkles, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { fromLocalDateTimeInput, toLocalDateTimeInput } from '@/lib/utils'
import type { GenerateScheduleRequest } from '../types'

interface GenerateScheduleModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  isSubmitting: boolean
  errorMessage: string | null
  onSubmit: (req: GenerateScheduleRequest) => Promise<void>
}

export function GenerateScheduleModal({
  open,
  onOpenChange,
  isSubmitting,
  errorMessage,
  onSubmit,
}: GenerateScheduleModalProps) {
  const defaultStart = new Date()
  defaultStart.setMinutes(0, 0, 0)
  defaultStart.setHours(defaultStart.getHours() + 1)

  const defaultEnd = new Date(defaultStart)
  defaultEnd.setDate(defaultEnd.getDate() + 7)

  const [windowStart, setWindowStart] = useState<string>(() =>
    toLocalDateTimeInput(defaultStart.toISOString()),
  )
  const [windowEnd, setWindowEnd] = useState<string>(() =>
    toLocalDateTimeInput(defaultEnd.toISOString()),
  )
  const [taskFilterRaw, setTaskFilterRaw] = useState<string>('')
  const [localError, setLocalError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setLocalError(null)

    const startIso = fromLocalDateTimeInput(windowStart)
    const endIso = fromLocalDateTimeInput(windowEnd)

    if (new Date(startIso) >= new Date(endIso)) {
      setLocalError('End time must be strictly after start time.')
      return
    }

    const parsedIds = taskFilterRaw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .map(Number)
      .filter((n) => Number.isInteger(n) && n > 0)

    await onSubmit({
      windowStart: startIso,
      windowEnd: endIso,
      includeTaskIds: parsedIds.length > 0 ? parsedIds : undefined,
    })
  }

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Generate Smart Schedule"
      description="Automatically place eligible tasks into your available time slots while respecting fixed events and deadlines."
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {(localError || errorMessage) && (
          <div className="flex items-start gap-2.5 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
            <div>{localError ?? errorMessage}</div>
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-slate-600">
              Window Start
            </label>
            <input
              type="datetime-local"
              required
              value={windowStart}
              onChange={(e) => setWindowStart(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-slate-600">
              Window End
            </label>
            <input
              type="datetime-local"
              required
              value={windowEnd}
              onChange={(e) => setWindowEnd(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold uppercase tracking-wide text-slate-600">
            Specific Task IDs (Optional)
          </label>
          <input
            type="text"
            placeholder="e.g. 12, 15, 20 (leave blank for all eligible tasks)"
            value={taskFilterRaw}
            onChange={(e) => setTaskFilterRaw(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          />
        </div>

        <div className="flex items-center justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            <Sparkles className="h-4 w-4" />
            {isSubmitting ? 'Generating...' : 'Run Smart Scheduler'}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}

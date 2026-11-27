import { useState, type FormEvent } from 'react'
import { Lock, Trash2, Save, AlertCircle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { formatTimeRange, fromLocalDateTimeInput, toLocalDateTimeInput } from '@/lib/utils'
import type { ProposedSession } from '../types'

interface EditSessionModalProps {
  session: ProposedSession | null
  batchVersion: number | null
  isBusy: boolean
  errorMessage: string | null
  onClose: () => void
  onSave: (
    sessionId: number,
    startAtIso: string,
    endAtIso: string,
    batchVersion: number,
  ) => Promise<void>
  onDelete: (sessionId: number, batchVersion: number) => Promise<void>
}

export function EditSessionModal({
  session,
  batchVersion,
  isBusy,
  errorMessage,
  onClose,
  onSave,
  onDelete,
}: EditSessionModalProps) {
  const [startLocal, setStartLocal] = useState(() =>
    session ? toLocalDateTimeInput(session.startAt) : '',
  )
  const [endLocal, setEndLocal] = useState(() =>
    session ? toLocalDateTimeInput(session.endAt) : '',
  )
  const [localError, setLocalError] = useState<string | null>(null)

  if (!session) return null

  const isProposed = session.status === 'PROPOSED'

  async function handleSave(e: FormEvent) {
    e.preventDefault()
    if (!session || batchVersion === null) return
    setLocalError(null)

    const startIso = fromLocalDateTimeInput(startLocal)
    const endIso = fromLocalDateTimeInput(endLocal)
    if (new Date(startIso) >= new Date(endIso)) {
      setLocalError('End time must be after start time.')
      return
    }

    await onSave(session.sessionId, startIso, endIso, batchVersion)
  }

  async function handleDelete() {
    if (!session || batchVersion === null) return
    await onDelete(session.sessionId, batchVersion)
  }

  return (
    <Dialog
      open={Boolean(session)}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      title={session.taskTitle}
      description={`Task #${session.taskId} • Session #${session.sessionId}`}
    >
      <div className="space-y-4">
        <div className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2">
          <span className="text-xs font-medium text-slate-500">Current Status</span>
          <Badge variant={isProposed ? 'proposed' : 'confirmed'}>
            {isProposed ? 'PROPOSED (Suggestion)' : `${session.status} (Protected)`}
          </Badge>
        </div>

        {(localError || errorMessage) && (
          <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
            <div>{localError ?? errorMessage}</div>
          </div>
        )}

        {isProposed ? (
          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wide text-slate-600">
                  Start Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={startLocal}
                  onChange={(e) => setStartLocal(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wide text-slate-600">
                  End Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={endLocal}
                  onChange={(e) => setEndLocal(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                />
              </div>
            </div>

            <div className="flex items-center justify-between gap-2 pt-2">
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={handleDelete}
                disabled={isBusy || batchVersion === null}
              >
                <Trash2 className="h-4 w-4" />
                Remove Session
              </Button>
              <div className="flex items-center gap-2">
                <Button type="button" variant="outline" onClick={onClose} disabled={isBusy}>
                  Close
                </Button>
                <Button type="submit" disabled={isBusy || batchVersion === null}>
                  <Save className="h-4 w-4" />
                  {isBusy ? 'Saving...' : 'Save Changes'}
                </Button>
              </div>
            </div>
          </form>
        ) : (
          <div className="space-y-4">
            <div className="flex items-start gap-2.5 rounded-lg border border-emerald-200 bg-emerald-50/80 p-3 text-sm text-emerald-900">
              <Lock className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              <div>
                Confirmed sessions are protected on your calendar (
                {formatTimeRange(session.startAt, session.endAt)}). Generate a new proposal if you
                want to replan remaining tasks.
              </div>
            </div>
            <div className="flex justify-end">
              <Button type="button" variant="outline" onClick={onClose}>
                Close
              </Button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  )
}

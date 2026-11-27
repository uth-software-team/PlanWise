import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, KeyRound, RefreshCw, Sparkles, AlertCircle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getAuthToken } from '@/lib/apiClient'
import {
  INITIAL_SCHEDULE_STATE,
  createScheduleController,
  deriveScheduleView,
  type ScheduleState,
} from '../api/scheduleApi'
import type { GenerateScheduleRequest, ProposedSession } from '../types'
import { CalendarGrid } from './CalendarGrid'
import { EditSessionModal } from './EditSessionModal'
import { GenerateScheduleModal } from './GenerateScheduleModal'
import { ProposalReviewBar } from './ProposalReviewBar'

function getVisibleRange(anchor: Date, mode: 'week' | 'day'): { start: Date; end: Date } {
  const start = new Date(anchor)
  if (mode === 'week') {
    const day = start.getDay()
    const diff = day === 0 ? -6 : 1 - day
    start.setDate(start.getDate() + diff)
  }
  start.setHours(0, 0, 0, 0)

  const end = new Date(start)
  end.setDate(start.getDate() + (mode === 'week' ? 7 : 1))
  return { start, end }
}

export function SchedulePage() {
  const [viewMode, setViewMode] = useState<'week' | 'day'>('week')
  const [anchorDate, setAnchorDate] = useState<Date>(() => new Date())

  // Server data, errors and busy state live in the controller (bound to the current account).
  const [schedule, setSchedule] = useState<ScheduleState>(INITIAL_SCHEDULE_STATE)

  // Interactive modals (UI only)
  const [isGenerateOpen, setIsGenerateOpen] = useState<boolean>(false)
  const [selectedSession, setSelectedSession] = useState<ProposedSession | null>(null)

  // Quick JWT token bar for standalone local testing
  const [tokenInput, setTokenInput] = useState<string>(() => getAuthToken())
  const [showTokenBar, setShowTokenBar] = useState<boolean>(() => !getAuthToken())

  const controller = useMemo(
    () =>
      createScheduleController(setSchedule, {
        // Another account (this tab or another one): close everything that belonged to the previous one.
        onAccountChange(token) {
          setSelectedSession(null)
          setIsGenerateOpen(false)
          setTokenInput(token)
        },
      }),
    [],
  )
  const view = useMemo(() => deriveScheduleView(schedule), [schedule])
  const activeProposal = schedule.proposal

  useEffect(() => {
    // Token replaced or removed in another tab (storage.clear() has key === null).
    function onStorage() {
      void controller.syncWithStorage().done
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [controller])

  const visibleRange = useMemo(() => getVisibleRange(anchorDate, viewMode), [anchorDate, viewMode])

  useEffect(() => {
    // Deferred so the effect itself never sets state synchronously.
    void Promise.resolve().then(() =>
      controller.setRange(visibleRange.start.toISOString(), visibleRange.end.toISOString()),
    )
  }, [controller, visibleRange])

  const conflictSessionIds = useMemo(() => {
    const ids = new Set<number>()
    for (const c of schedule.conflicts) {
      if (c.sessionId !== null) ids.add(c.sessionId)
    }
    return ids
  }, [schedule.conflicts])

  function handleNavigate(direction: -1 | 0 | 1) {
    if (direction === 0) {
      setAnchorDate(new Date())
      return
    }
    const next = new Date(anchorDate)
    next.setDate(next.getDate() + direction * (viewMode === 'week' ? 7 : 1))
    setAnchorDate(next)
  }

  // Each handler only closes its modal when the action succeeded for the account that started it.
  async function handleGenerate(req: GenerateScheduleRequest) {
    if (await controller.generate(req)) setIsGenerateOpen(false)
  }

  async function handleEditSession(
    sessionId: number,
    startAtIso: string,
    endAtIso: string,
    batchVersion: number,
  ) {
    if (await controller.editSession(sessionId, startAtIso, endAtIso, batchVersion)) {
      setSelectedSession(null)
    }
  }

  async function handleDeleteSession(sessionId: number, batchVersion: number) {
    if (await controller.deleteSession(sessionId, batchVersion)) setSelectedSession(null)
  }

  function handleApplyToken() {
    // A different token closes the previous account's modals via onAccountChange.
    void controller.applyToken(tokenInput).done
  }

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Top Navbar */}
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white">
              PW
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight text-slate-900">
                PlanWise Calendar & Smart Schedule
              </h1>
              <p className="text-xs text-slate-500">
                AI-assisted constraint scheduling • Review & confirm before committing
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setShowTokenBar((v) => !v)}
            >
              <KeyRound className="h-4 w-4" />
              JWT Token
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void controller.refresh()}
              disabled={schedule.calendar.status === 'loading'}
            >
              <RefreshCw
                className={`h-4 w-4 ${schedule.calendar.status === 'loading' ? 'animate-spin' : ''}`}
              />
              Refresh
            </Button>
            <Button
              type="button"
              onClick={() => {
                controller.dismiss('generateError')
                setIsGenerateOpen(true)
              }}
            >
              <Sparkles className="h-4 w-4" />
              Generate Smart Schedule
            </Button>
          </div>
        </div>

        {/* Optional JWT Token Bar for Standalone Testing */}
        {showTokenBar && (
          <div className="border-t border-slate-200 bg-slate-100/80 px-4 py-2.5 sm:px-6">
            <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-2">
              <span className="text-xs font-medium text-slate-600">Bearer Token:</span>
              <input
                type="password"
                placeholder="Paste JWT token from /v1/auth/login..."
                value={tokenInput}
                onChange={(e) => setTokenInput(e.target.value)}
                className="min-w-[260px] flex-1 rounded-md border border-slate-300 bg-white px-2.5 py-1 text-xs text-slate-900"
              />
              <Button type="button" size="sm" onClick={handleApplyToken}>
                Apply & Reload
              </Button>
            </div>
          </div>
        )}
      </header>

      <main className="mx-auto max-w-7xl space-y-4 px-4 py-6 sm:px-6">
        {/* Global Error State */}
        {schedule.pageError && (
          <div className="flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
            <div>{schedule.pageError}</div>
          </div>
        )}

        {/* Partial load failures: the rest of the page stays usable */}
        {[
          schedule.calendar.status === 'error' ? schedule.calendar.message : null,
          schedule.proposalError,
        ].map(
          (message) =>
            message && (
              <div
                key={message}
                role="status"
                className="flex items-start gap-2.5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                <div>{message}</div>
              </div>
            ),
        )}

        {/* FE-SC-03: Active Proposal Review & Conflict Resolution Bar */}
        {activeProposal && (
          <ProposalReviewBar
            proposal={activeProposal}
            conflicts={schedule.conflicts}
            isBusy={schedule.isBusy}
            onConfirm={async () => {
              await controller.confirm()
            }}
            onDiscard={async () => {
              await controller.discard()
            }}
          />
        )}

        {/* FE-CAL Toolbar: Date Navigation, View Switcher & Legend */}
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-xs">
          <div className="flex items-center gap-2">
            <Button type="button" variant="outline" size="icon" onClick={() => handleNavigate(-1)}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => handleNavigate(0)}>
              Today
            </Button>
            <Button type="button" variant="outline" size="icon" onClick={() => handleNavigate(1)}>
              <ChevronRight className="h-4 w-4" />
            </Button>
            <span className="ml-2 text-sm font-semibold text-slate-800">
              {visibleRange.start.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
              })}{' '}
              –{' '}
              {new Date(visibleRange.end.getTime() - 86400000).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              })}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="hidden items-center gap-2 sm:flex">
              <Badge variant="proposed">Proposed (Draft)</Badge>
              <Badge variant="confirmed">Confirmed (Protected)</Badge>
            </div>

            <div className="inline-flex rounded-lg border border-slate-200 bg-slate-100 p-0.5">
              <button
                type="button"
                onClick={() => setViewMode('week')}
                className={`rounded-md px-3 py-1 text-xs font-semibold transition-colors cursor-pointer ${
                  viewMode === 'week'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Week
              </button>
              <button
                type="button"
                onClick={() => setViewMode('day')}
                className={`rounded-md px-3 py-1 text-xs font-semibold transition-colors cursor-pointer ${
                  viewMode === 'day'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Day
              </button>
            </div>
          </div>
        </div>

        {/* Loading State vs Calendar Grid */}
        {view.showSpinner ? (
          <div className="flex h-96 items-center justify-center rounded-xl border border-slate-200 bg-white">
            <div className="flex items-center gap-2.5 text-sm font-medium text-slate-500">
              <RefreshCw className="h-5 w-5 animate-spin text-indigo-600" />
              <span>Loading calendar & schedule proposals...</span>
            </div>
          </div>
        ) : (
          <>
            {view.pendingNote && (
              <div role="status" className="flex items-center gap-2 text-xs text-slate-500">
                <RefreshCw className="h-3.5 w-3.5 animate-spin text-indigo-600" />
                <span>{view.pendingNote}</span>
              </div>
            )}
            <CalendarGrid
              viewMode={viewMode}
              anchorDate={anchorDate}
              sessions={view.sessions}
              showEmptyState={view.showEmptyState}
              conflictSessionIds={conflictSessionIds}
              onSelectSession={(s) => {
                controller.dismiss('sessionError')
                setSelectedSession(s)
              }}
            />
          </>
        )}
      </main>

      {/* FE-SC-01: Generate Schedule Modal */}
      <GenerateScheduleModal
        open={isGenerateOpen}
        onOpenChange={setIsGenerateOpen}
        isSubmitting={schedule.isBusy}
        errorMessage={schedule.generateError}
        onSubmit={handleGenerate}
      />

      {/* FE-SC-02: Edit / Remove Session Modal */}
      <EditSessionModal
        key={selectedSession?.sessionId ?? 'closed'}
        session={selectedSession}
        batchVersion={activeProposal?.version ?? null}
        isBusy={schedule.isBusy}
        errorMessage={schedule.sessionError}
        onClose={() => setSelectedSession(null)}
        onSave={handleEditSession}
        onDelete={handleDeleteSession}
      />
    </div>
  )
}

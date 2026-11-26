import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  KeyRound,
  RefreshCw,
  Sparkles,
  AlertCircle,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ApiError, getAuthToken, setAuthToken, type ConflictErrorDto } from '@/lib/apiClient';
import { scheduleApi } from '../api/scheduleApi';
import type { GenerateScheduleRequest, ProposalBatchResponse, ProposedSession } from '../types';
import { CalendarGrid } from './CalendarGrid';
import { EditSessionModal } from './EditSessionModal';
import { GenerateScheduleModal } from './GenerateScheduleModal';
import { ProposalReviewBar } from './ProposalReviewBar';

function getVisibleRange(anchor: Date, mode: 'week' | 'day'): { start: Date; end: Date } {
  const start = new Date(anchor);
  if (mode === 'week') {
    const day = start.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    start.setDate(start.getDate() + diff);
  }
  start.setHours(0, 0, 0, 0);

  const end = new Date(start);
  end.setDate(start.getDate() + (mode === 'week' ? 7 : 1));
  return { start, end };
}

export function SchedulePage() {
  const [viewMode, setViewMode] = useState<'week' | 'day'>('week');
  const [anchorDate, setAnchorDate] = useState<Date>(() => new Date());

  // 4-state lifecycle: loading, error, data (committedSessions + activeProposal)
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [committedSessions, setCommittedSessions] = useState<ProposedSession[]>([]);
  const [activeProposal, setActiveProposal] = useState<ProposalBatchResponse | null>(null);

  // Interactive modals & action state
  const [isGenerateOpen, setIsGenerateOpen] = useState<boolean>(false);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<ProposedSession | null>(null);
  const [sessionError, setSessionError] = useState<string | null>(null);
  const [conflicts, setConflicts] = useState<ConflictErrorDto[]>([]);
  const [isActionBusy, setIsActionBusy] = useState<boolean>(false);

  // Quick JWT token bar for standalone local testing
  const [tokenInput, setTokenInput] = useState<string>(() => getAuthToken());
  const [showTokenBar, setShowTokenBar] = useState<boolean>(() => !getAuthToken());

  const visibleRange = useMemo(() => getVisibleRange(anchorDate, viewMode), [anchorDate, viewMode]);

  const loadScheduleData = useCallback(async () => {
    setIsLoading(true);
    setPageError(null);
    try {
      const [committed, currentProposal] = await Promise.all([
        scheduleApi.listCommittedSessions(
          visibleRange.start.toISOString(),
          visibleRange.end.toISOString(),
        ),
        scheduleApi.getCurrentProposal(),
      ]);
      setCommittedSessions(committed);
      setActiveProposal(currentProposal);
    } catch (err) {
      if (err instanceof ApiError) {
        setPageError(`${err.code}: ${err.message}`);
      } else {
        setPageError('Unable to load schedule data. Verify backend server is running.');
      }
    } finally {
      setIsLoading(false);
    }
  }, [visibleRange]);

  useEffect(() => {
    void loadScheduleData();
  }, [loadScheduleData]);

  const combinedSessions = useMemo(() => {
    const proposed = activeProposal?.sessions ?? [];
    return [...committedSessions, ...proposed];
  }, [committedSessions, activeProposal]);

  const conflictSessionIds = useMemo(() => {
    const ids = new Set<number>();
    for (const c of conflicts) {
      if (c.sessionId !== null) ids.add(c.sessionId);
    }
    return ids;
  }, [conflicts]);

  function handleNavigate(direction: -1 | 0 | 1) {
    if (direction === 0) {
      setAnchorDate(new Date());
      return;
    }
    const next = new Date(anchorDate);
    next.setDate(next.getDate() + direction * (viewMode === 'week' ? 7 : 1));
    setAnchorDate(next);
  }

  async function handleGenerate(req: GenerateScheduleRequest) {
    setIsActionBusy(true);
    setGenerateError(null);
    setConflicts([]);
    try {
      const batch = await scheduleApi.generateProposal(req);
      setActiveProposal(batch);
      setIsGenerateOpen(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setGenerateError(
          err.code === 'NO_AVAILABILITY_RULE'
            ? 'No active availability rules found. Please configure your weekly study/work hours first.'
            : `${err.code}: ${err.message}`,
        );
      } else {
        setGenerateError('Failed to generate schedule proposal.');
      }
    } finally {
      setIsActionBusy(false);
    }
  }

  async function handleEditSession(
    sessionId: number,
    startAtIso: string,
    endAtIso: string,
    batchVersion: number,
  ) {
    setIsActionBusy(true);
    setSessionError(null);
    setConflicts([]);
    try {
      const updated = await scheduleApi.editSession(sessionId, {
        startAt: startAtIso,
        endAt: endAtIso,
        version: batchVersion,
      });
      setActiveProposal(updated);
      setSelectedSession(null);
    } catch (err) {
      if (err instanceof ApiError) {
        setSessionError(`${err.code}: ${err.message}`);
        if (err.code === 'STALE_VERSION') {
          void loadScheduleData();
        }
      } else {
        setSessionError('Failed to update session.');
      }
    } finally {
      setIsActionBusy(false);
    }
  }

  async function handleDeleteSession(sessionId: number, batchVersion: number) {
    setIsActionBusy(true);
    setSessionError(null);
    setConflicts([]);
    try {
      const updated = await scheduleApi.deleteSession(sessionId, batchVersion);
      setActiveProposal(updated);
      setSelectedSession(null);
    } catch (err) {
      if (err instanceof ApiError) {
        setSessionError(`${err.code}: ${err.message}`);
      } else {
        setSessionError('Failed to remove session.');
      }
    } finally {
      setIsActionBusy(false);
    }
  }

  async function handleConfirmProposal() {
    if (!activeProposal) return;
    setIsActionBusy(true);
    setConflicts([]);
    setPageError(null);
    try {
      await scheduleApi.confirmProposal(activeProposal.proposalId, {
        version: activeProposal.version,
      });
      setActiveProposal(null);
      await loadScheduleData();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.conflicts.length > 0) {
          setConflicts(err.conflicts);
        } else {
          setPageError(`${err.code}: ${err.message}`);
        }
      } else {
        setPageError('Failed to confirm schedule.');
      }
    } finally {
      setIsActionBusy(false);
    }
  }

  async function handleDiscardProposal() {
    if (!activeProposal) return;
    setIsActionBusy(true);
    setConflicts([]);
    try {
      await scheduleApi.discardProposal(activeProposal.proposalId, activeProposal.version);
      setActiveProposal(null);
    } catch (err) {
      if (err instanceof ApiError) {
        setPageError(`${err.code}: ${err.message}`);
      }
    } finally {
      setIsActionBusy(false);
    }
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
              onClick={() => void loadScheduleData()}
              disabled={isLoading}
            >
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button
              type="button"
              onClick={() => {
                setGenerateError(null);
                setIsGenerateOpen(true);
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
              <Button
                type="button"
                size="sm"
                onClick={() => {
                  setAuthToken(tokenInput);
                  void loadScheduleData();
                }}
              >
                Apply & Reload
              </Button>
            </div>
          </div>
        )}
      </header>

      <main className="mx-auto max-w-7xl space-y-4 px-4 py-6 sm:px-6">
        {/* Global Error State */}
        {pageError && (
          <div className="flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
            <div>{pageError}</div>
          </div>
        )}

        {/* FE-SC-03: Active Proposal Review & Conflict Resolution Bar */}
        {activeProposal && (
          <ProposalReviewBar
            proposal={activeProposal}
            conflicts={conflicts}
            isBusy={isActionBusy}
            onConfirm={handleConfirmProposal}
            onDiscard={handleDiscardProposal}
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
        {isLoading ? (
          <div className="flex h-96 items-center justify-center rounded-xl border border-slate-200 bg-white">
            <div className="flex items-center gap-2.5 text-sm font-medium text-slate-500">
              <RefreshCw className="h-5 w-5 animate-spin text-indigo-600" />
              <span>Loading calendar & schedule proposals...</span>
            </div>
          </div>
        ) : (
          <CalendarGrid
            viewMode={viewMode}
            anchorDate={anchorDate}
            sessions={combinedSessions}
            conflictSessionIds={conflictSessionIds}
            onSelectSession={(s) => {
              setSessionError(null);
              setSelectedSession(s);
            }}
          />
        )}
      </main>

      {/* FE-SC-01: Generate Schedule Modal */}
      <GenerateScheduleModal
        open={isGenerateOpen}
        onOpenChange={setIsGenerateOpen}
        isSubmitting={isActionBusy}
        errorMessage={generateError}
        onSubmit={handleGenerate}
      />

      {/* FE-SC-02: Edit / Remove Session Modal */}
      <EditSessionModal
        session={selectedSession}
        batchVersion={activeProposal?.version ?? null}
        isBusy={isActionBusy}
        errorMessage={sessionError}
        onClose={() => setSelectedSession(null)}
        onSave={handleEditSession}
        onDelete={handleDeleteSession}
      />
    </div>
  );
}

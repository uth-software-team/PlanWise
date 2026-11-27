import { useMemo } from 'react';
import { Calendar as CalendarIcon, Lock, Sparkles } from 'lucide-react';
import { cn, formatTimeRange } from '@/lib/utils';
import type { ProposedSession, ScheduleGridSession } from '../types';

interface CalendarGridProps {
  viewMode: 'week' | 'day';
  anchorDate: Date;
  sessions: ScheduleGridSession[];
  /** Show "no sessions" only when the calendar loaded successfully; otherwise render the empty grid. */
  showEmptyState: boolean;
  conflictSessionIds: Set<number>;
  /** Only proposal sessions are selectable; committed calendar sessions are read-only. */
  onSelectSession: (session: ProposedSession) => void;
}

function isProposalSession(session: ScheduleGridSession): session is ProposedSession {
  return 'version' in session;
}

const START_HOUR = 6;
const END_HOUR = 23;
const HOUR_HEIGHT = 56; // px per hour

function getWeekDays(anchor: Date, mode: 'week' | 'day'): Date[] {
  if (mode === 'day') {
    const d = new Date(anchor);
    d.setHours(0, 0, 0, 0);
    return [d];
  }
  const d = new Date(anchor);
  const day = d.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diffToMonday);
  d.setHours(0, 0, 0, 0);

  return Array.from({ length: 7 }, (_, idx) => {
    const item = new Date(d);
    item.setDate(d.getDate() + idx);
    return item;
  });
}

function isSameLocalDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function CalendarGrid({
  viewMode,
  anchorDate,
  sessions,
  showEmptyState,
  conflictSessionIds,
  onSelectSession,
}: CalendarGridProps) {
  const days = useMemo(() => getWeekDays(anchorDate, viewMode), [anchorDate, viewMode]);
  const hours = useMemo(
    () => Array.from({ length: END_HOUR - START_HOUR + 1 }, (_, i) => START_HOUR + i),
    [],
  );
  const today = new Date();

  if (sessions.length === 0 && showEmptyState) {
    return (
      <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
        <div className="rounded-full bg-indigo-50 p-3 text-indigo-600">
          <CalendarIcon className="h-6 w-6" />
        </div>
        <h3 className="mt-4 text-base font-semibold text-slate-900">
          No sessions scheduled in this period
        </h3>
        <p className="mt-1 max-w-md text-sm text-slate-500">
          Click <span className="font-medium text-indigo-600">Generate Smart Schedule</span> to
          automatically fit your pending tasks into your available time slots.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-xs">
      <div className={cn('min-w-[720px]', viewMode === 'day' && 'min-w-full')}>
        {/* Header row */}
        <div
          className={cn(
            'grid border-b border-slate-200 bg-slate-50/80',
            viewMode === 'week'
              ? 'grid-cols-[64px_repeat(7,minmax(0,1fr))]'
              : 'grid-cols-[64px_1fr]',
          )}
        >
          <div className="border-r border-slate-200 px-2 py-3 text-center text-xs font-medium text-slate-400">
            GMT{new Date().getTimezoneOffset() <= 0 ? '+' : '-'}
            {Math.abs(Math.round(new Date().getTimezoneOffset() / 60))}
          </div>
          {days.map((day) => {
            const isToday = isSameLocalDay(day, today);
            return (
              <div
                key={day.toISOString()}
                className={cn(
                  'border-r border-slate-200 px-3 py-2.5 text-center last:border-r-0',
                  isToday && 'bg-indigo-50/60',
                )}
              >
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  {day.toLocaleDateString('en-US', { weekday: 'short' })}
                </div>
                <div
                  className={cn(
                    'mx-auto mt-1 flex h-7 w-7 items-center justify-center rounded-full text-sm font-semibold',
                    isToday ? 'bg-indigo-600 text-white' : 'text-slate-800',
                  )}
                >
                  {day.getDate()}
                </div>
              </div>
            );
          })}
        </div>

        {/* Time grid */}
        <div
          className={cn(
            'relative grid',
            viewMode === 'week'
              ? 'grid-cols-[64px_repeat(7,minmax(0,1fr))]'
              : 'grid-cols-[64px_1fr]',
          )}
          style={{ height: `${hours.length * HOUR_HEIGHT}px` }}
        >
          {/* Hour labels column */}
          <div className="border-r border-slate-200 bg-slate-50/40">
            {hours.map((h) => (
              <div
                key={h}
                style={{ height: `${HOUR_HEIGHT}px` }}
                className="relative border-b border-slate-100 pr-2 text-right text-[11px] font-medium text-slate-400"
              >
                <span className="-top-2 relative">{`${String(h).padStart(2, '0')}:00`}</span>
              </div>
            ))}
          </div>

          {/* Day columns */}
          {days.map((day) => {
            const daySessions = sessions.filter((s) => isSameLocalDay(new Date(s.startAt), day));
            return (
              <div
                key={day.toISOString()}
                className="relative border-r border-slate-200 last:border-r-0"
              >
                {hours.map((h) => (
                  <div
                    key={h}
                    style={{ height: `${HOUR_HEIGHT}px` }}
                    className="border-b border-slate-100"
                  />
                ))}

                {daySessions.map((session) => {
                  const start = new Date(session.startAt);
                  const end = new Date(session.endAt);
                  const startMins = (start.getHours() - START_HOUR) * 60 + start.getMinutes();
                  const durationMins = Math.max(
                    25,
                    Math.round((end.getTime() - start.getTime()) / 60000),
                  );
                  const topPx = Math.max(0, (startMins / 60) * HOUR_HEIGHT);
                  const heightPx = Math.max(28, (durationMins / 60) * HOUR_HEIGHT);

                  const isProposed = session.status === 'PROPOSED';
                  const hasConflict = conflictSessionIds.has(session.sessionId);
                  const content = (
                    <>
                      <div className="flex items-center justify-between gap-1 font-semibold leading-tight">
                        <span className="truncate">{session.taskTitle}</span>
                        {isProposed ? (
                          <Sparkles className="h-3.5 w-3.5 shrink-0 text-amber-600" />
                        ) : (
                          <Lock className="h-3 w-3 shrink-0 text-emerald-600" />
                        )}
                      </div>
                      <div className="truncate text-[11px] opacity-80">
                        {formatTimeRange(session.startAt, session.endAt)}
                      </div>
                    </>
                  );
                  const style = { top: `${topPx}px`, height: `${heightPx}px` };
                  const baseClass =
                    'absolute inset-x-1.5 z-10 flex flex-col justify-between overflow-hidden rounded-lg px-2.5 py-1.5 text-left text-xs shadow-xs';
                  const conflictClass =
                    hasConflict && 'ring-2 ring-rose-500 border-rose-500 bg-rose-50 text-rose-950';

                  if (!isProposalSession(session)) {
                    return (
                      <div
                        key={`calendar-${session.sessionId}`}
                        style={style}
                        title={`${session.status} (read-only)`}
                        className={cn(
                          baseClass,
                          'border border-emerald-300 bg-emerald-50/95 text-emerald-950',
                          conflictClass,
                        )}
                      >
                        {content}
                      </div>
                    );
                  }

                  return (
                    <button
                      key={`proposal-${session.sessionId}`}
                      type="button"
                      onClick={() => onSelectSession(session)}
                      style={style}
                      className={cn(
                        baseClass,
                        'transition-all cursor-pointer',
                        isProposed
                          ? 'border-2 border-dashed border-amber-400 bg-amber-50/95 text-amber-950 hover:bg-amber-100'
                          : 'border border-emerald-300 bg-emerald-50/95 text-emerald-950 hover:bg-emerald-100',
                        conflictClass,
                      )}
                    >
                      {content}
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

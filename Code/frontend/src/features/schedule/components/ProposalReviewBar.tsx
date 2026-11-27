import { CheckCircle2, Trash2, AlertTriangle, Clock } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import type { ConflictErrorDto } from '@/lib/apiClient';
import type { ProposalBatchResponse } from '../types';

interface ProposalReviewBarProps {
  proposal: ProposalBatchResponse;
  conflicts: ConflictErrorDto[];
  isBusy: boolean;
  onConfirm: () => Promise<void>;
  onDiscard: () => Promise<void>;
}

export function ProposalReviewBar({
  proposal,
  conflicts,
  isBusy,
  onConfirm,
  onDiscard,
}: ProposalReviewBarProps) {
  return (
    <div className="space-y-3">
      {/* Main action banner */}
      <div className="flex flex-col justify-between gap-4 rounded-xl border border-amber-300 bg-amber-50/90 p-4 shadow-xs sm:flex-row sm:items-center">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="proposed">Draft Proposal #{proposal.proposalId}</Badge>
            <span className="text-xs font-medium text-amber-800">
              Batch v{proposal.version} • {proposal.sessions.length} suggested session(s)
            </span>
          </div>
          <p className="text-sm text-amber-950">
            Review the dashed amber blocks on your calendar. Click any suggested session to adjust
            its time or remove it before confirming.
          </p>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={onDiscard}
            disabled={isBusy}
            className="border-amber-300 bg-white text-amber-900 hover:bg-amber-100"
          >
            <Trash2 className="h-4 w-4" />
            Discard
          </Button>
          <Button
            type="button"
            onClick={onConfirm}
            disabled={isBusy || proposal.sessions.length === 0}
            className="bg-emerald-600 hover:bg-emerald-700"
          >
            <CheckCircle2 className="h-4 w-4" />
            {isBusy ? 'Confirming...' : 'Confirm Schedule'}
          </Button>
        </div>
      </div>

      {/* Unscheduled tasks warning (INSUFFICIENT_TIME / DEADLINE_PASSED) */}
      {proposal.unscheduledTasks.length > 0 && (
        <div className="rounded-xl border border-orange-200 bg-orange-50/80 p-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-orange-900">
            <Clock className="h-4 w-4 text-orange-600" />
            <span>
              {proposal.unscheduledTasks.length} task(s) could not be fully scheduled in this window
            </span>
          </div>
          <ul className="mt-2 space-y-1.5 text-xs text-orange-900">
            {proposal.unscheduledTasks.map((u) => (
              <li
                key={u.taskId}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-white/80 px-3 py-1.5 border border-orange-200/70"
              >
                <span className="font-medium">
                  #{u.taskId} — {u.taskTitle}
                </span>
                <span className="text-orange-700">
                  {u.reason === 'DEADLINE_PASSED'
                    ? 'Deadline already passed'
                    : `Insufficient available time (${u.missingMinutes} min unscheduled)`}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* HC-01..HC-08 Validation Conflicts (409 CONSTRAINT_VIOLATION) */}
      {conflicts.length > 0 && (
        <div className="rounded-xl border border-rose-300 bg-rose-50 p-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-rose-900">
            <AlertTriangle className="h-4 w-4 text-rose-600" />
            <span>Cannot confirm batch — {conflicts.length} constraint conflict(s) detected:</span>
          </div>
          <ul className="mt-2 space-y-1.5 text-xs text-rose-900">
            {conflicts.map((c, idx) => (
              <li
                key={`${c.code}-${c.sessionId ?? idx}`}
                className="flex items-center justify-between gap-2 rounded-lg border border-rose-200 bg-white px-3 py-1.5"
              >
                <span>
                  {c.sessionId !== null && (
                    <strong className="mr-1.5 font-semibold">[Session #{c.sessionId}]</strong>
                  )}
                  {c.message}
                </span>
                <Badge variant="danger">{c.code}</Badge>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export type ProposalStatus = 'PROPOSED' | 'CONFIRMED' | 'CANCELLED';
export type SessionStatus = 'PROPOSED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
export type UnscheduledReason = 'DEADLINE_PASSED' | 'INSUFFICIENT_TIME';

export interface ProposedSession {
  sessionId: number;
  taskId: number;
  taskTitle: string;
  startAt: string;
  endAt: string;
  status: SessionStatus;
  version: number;
}

export type CommittedSessionStatus = 'CONFIRMED' | 'COMPLETED';

/** A committed session from GET /v1/calendar (`CalendarSession`). Read-only: it carries no version. */
export interface CommittedSession {
  sessionId: number;
  taskId: number;
  taskTitle: string;
  startAt: string;
  endAt: string;
  status: CommittedSessionStatus;
}

/** What the calendar grid renders: editable proposal sessions and read-only committed sessions. */
export type ScheduleGridSession = ProposedSession | CommittedSession;

/** `data` of GET /v1/calendar. Fixed events are not rendered by the schedule page yet (FE-CAL). */
export interface CalendarResponse {
  start: string;
  end: string;
  timezone: string;
  sessions: Array<Omit<CommittedSession, 'status'> & { status: string }>;
  fixedEvents: unknown[];
}

export interface UnscheduledTask {
  taskId: number;
  taskTitle: string;
  reason: UnscheduledReason;
  missingMinutes: number;
}

export interface ProposalBatchResponse {
  proposalId: number;
  version: number;
  status: ProposalStatus;
  horizonStart: string;
  horizonEnd: string;
  sessions: ProposedSession[];
  unscheduledTasks: UnscheduledTask[];
  createdAt: string;
  confirmedAt: string | null;
}

export interface GenerateScheduleRequest {
  windowStart: string;
  windowEnd: string;
  includeTaskIds?: number[];
}

export interface SessionEditRequest {
  startAt: string;
  endAt: string;
  version: number;
}

export interface ConfirmRequest {
  version: number;
}

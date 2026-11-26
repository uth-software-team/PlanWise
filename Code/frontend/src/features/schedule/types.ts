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

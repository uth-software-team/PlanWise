import { clearToken, getToken, setToken } from '@/lib/auth-token';

export interface ConflictErrorDto {
  sessionId: number | null;
  code:
    | 'SESSION_OVERLAP'
    | 'FIXED_EVENT_OVERLAP'
    | 'OUTSIDE_AVAILABILITY'
    | 'PAST_TIME'
    | 'DEADLINE_EXCEEDED'
    | 'DURATION_EXCEEDED'
    | 'TASK_NOT_SCHEDULABLE'
    | 'INVALID_TIME_RANGE';
  message: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  code?: string;
  message?: string;
  data: T;
}

const CONFLICT_CODES: ReadonlySet<string> = new Set<ConflictErrorDto['code']>([
  'SESSION_OVERLAP',
  'FIXED_EVENT_OVERLAP',
  'OUTSIDE_AVAILABILITY',
  'PAST_TIME',
  'DEADLINE_EXCEEDED',
  'DURATION_EXCEEDED',
  'TASK_NOT_SCHEDULABLE',
  'INVALID_TIME_RANGE',
]);

function isConflictErrorDto(value: unknown): value is ConflictErrorDto {
  if (value === null || typeof value !== 'object') return false;
  const entry = value as Record<string, unknown>;
  return (
    (entry.sessionId === null ||
      (typeof entry.sessionId === 'number' && Number.isFinite(entry.sessionId))) &&
    typeof entry.code === 'string' &&
    CONFLICT_CODES.has(entry.code) &&
    typeof entry.message === 'string'
  );
}

/**
 * 409 CONSTRAINT_VIOLATION carries `data = { conflicts: [...] }` (CG-05). Keeps valid entries in order and
 * skips malformed ones individually; any other `data` shape yields no conflicts.
 */
function readConflicts(data: unknown): ConflictErrorDto[] {
  if (data === null || typeof data !== 'object') return [];
  const conflicts = (data as { conflicts?: unknown }).conflicts;
  if (!Array.isArray(conflicts)) return [];
  return conflicts.filter(isConflictErrorDto);
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly conflicts: ConflictErrorDto[];

  constructor(status: number, code: string, message: string, conflicts: ConflictErrorDto[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.conflicts = conflicts;
  }
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api/v1';

/** The current access token, or '' when signed out. The only store is `auth-token.ts` (CG-04). */
export function getAuthToken(): string {
  return getToken() ?? '';
}

/** A blank token signs out. */
export function setAuthToken(token: string): void {
  const next = token.trim();
  if (next) {
    setToken(next);
  } else {
    clearToken();
  }
}

/**
 * 401 for the token this request sent ends the session (CG-04: 401 → /login; the guards react to clearToken).
 * A 401 that arrives after the token was replaced (another login, another tab) leaves the new session alone.
 */
function endSessionOn401(status: number, sentToken: string): void {
  if (status === 401 && sentToken !== '' && getAuthToken() === sentToken) {
    clearToken();
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
  });

  // Before reading the body, so a 401 with an empty or non-JSON body also ends the session.
  endSessionOn401(response.status, token);

  if (response.status === 204) {
    return undefined as T;
  }

  let payload: ApiEnvelope<T>;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    if (!response.ok) {
      throw new ApiError(response.status, 'HTTP_ERROR', `Request failed (${response.status})`);
    }
    return undefined as T;
  }

  if (!response.ok || !payload.success) {
    throw new ApiError(
      response.status,
      payload.code ?? 'UNKNOWN_ERROR',
      payload.message ?? `Request failed (${response.status})`,
      readConflicts(payload.data),
    );
  }

  return payload.data;
}
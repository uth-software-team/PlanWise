export interface ConflictErrorDto {
  sessionId: number | null;
  code:
    | 'SESSION_OVERLAP'
    | 'FIXED_EVENT_OVERLAP'
    | 'OUTSIDE_AVAILABILITY'
    | 'PAST_TIME'
    | 'DEADLINE_EXCEEDED'
    | 'DURATION_EXCEEDED'
    | 'TASK_NOT_SCHEDULABLE';
  message: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  code?: string;
  message?: string;
  data: T;
  conflicts?: ConflictErrorDto[];
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

export function getAuthToken(): string {
  return localStorage.getItem('planwise_token') ?? '';
}

export function setAuthToken(token: string): void {
  if (token.trim()) {
    localStorage.setItem('planwise_token', token.trim());
  } else {
    localStorage.removeItem('planwise_token');
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

  if (response.status === 204) {
    return undefined as T;
  }

  let payload: ApiEnvelope<T> | null = null;
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
      payload.conflicts ?? [],
    );
  }

  return payload.data;
}

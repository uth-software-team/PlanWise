import { apiRequest } from '@/lib/apiClient'
import type {
  PageResponse,
  TaskCreateRequest,
  TaskListFilters,
  TaskResponse,
  TaskStatusRequest,
  TaskUpdateRequest,
} from '../types'

export function listTasks(filters: TaskListFilters = {}): Promise<PageResponse<TaskResponse>> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.priority) params.set('priority', filters.priority)
  if (filters.deadlineFrom) params.set('deadlineFrom', filters.deadlineFrom)
  if (filters.deadlineTo) params.set('deadlineTo', filters.deadlineTo)
  if (typeof filters.page === 'number') params.set('page', String(filters.page))
  if (typeof filters.size === 'number') params.set('size', String(filters.size))

  const query = params.toString()
  return apiRequest<PageResponse<TaskResponse>>(query ? `/tasks?${query}` : '/tasks')
}

export function getTask(taskId: number): Promise<TaskResponse> {
  return apiRequest<TaskResponse>(`/tasks/${taskId}`)
}

export function createTask(payload: TaskCreateRequest): Promise<TaskResponse> {
  return apiRequest<TaskResponse>('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateTask(taskId: number, payload: TaskUpdateRequest): Promise<TaskResponse> {
  return apiRequest<TaskResponse>(`/tasks/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function changeTaskStatus(
  taskId: number,
  payload: TaskStatusRequest,
): Promise<TaskResponse> {
  return apiRequest<TaskResponse>(`/tasks/${taskId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function deleteTask(taskId: number): Promise<void> {
  return apiRequest<void>(`/tasks/${taskId}`, {
    method: 'DELETE',
  })
}

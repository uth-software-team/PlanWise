export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TaskCategory = 'STUDY' | 'WORK' | 'PERSONAL' | 'PROJECT' | 'OTHER'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface TaskResponse {
  id: number
  groupId: number | null
  title: string
  description: string
  deadline: string | null
  priority: TaskPriority
  category: TaskCategory
  status: TaskStatus
  estimatedDurationMinutes: number
  actualDurationMinutes: number | null
  overdue: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export interface TaskCreateRequest {
  title: string
  description: string
  deadline: string | null
  priority: TaskPriority
  category: TaskCategory
  estimatedDurationMinutes: number
}

export interface TaskUpdateRequest {
  title: string
  description: string
  deadline: string | null
  priority: TaskPriority
  category: TaskCategory
  estimatedDurationMinutes: number
  actualDurationMinutes: number | null
  version: number
}

export interface TaskStatusRequest {
  status: TaskStatus
  version: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface TaskListFilters {
  status?: TaskStatus
  priority?: TaskPriority
  deadlineFrom?: string
  deadlineTo?: string
  page?: number
  size?: number
}

export const TASK_PRIORITIES: readonly TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
export const TASK_CATEGORIES: readonly TaskCategory[] = [
  'STUDY',
  'WORK',
  'PERSONAL',
  'PROJECT',
  'OTHER',
]
export const TASK_STATUSES: readonly TaskStatus[] = [
  'TODO',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
]

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
}

export const CATEGORY_LABELS: Record<TaskCategory, string> = {
  STUDY: 'Học tập',
  WORK: 'Công việc',
  PERSONAL: 'Cá nhân',
  PROJECT: 'Dự án',
  OTHER: 'Khác',
}

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'Cần làm',
  IN_PROGRESS: 'Đang làm',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
}

import { useCallback, useEffect, useRef, useState } from 'react'
import {
  AlertCircle,
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Clock,
  Eye,
  FilterX,
  ListTodo,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'
import {
  changeTaskStatus,
  createTask,
  deleteTask,
  getTask,
  listTasks,
  updateTask,
} from '../api/taskApi'
import { taskErrorMessageOf } from '../errors'
import {
  PRIORITY_LABELS,
  STATUS_LABELS,
  TASK_PRIORITIES,
  TASK_STATUSES,
  type PageResponse,
  type TaskCreateRequest,
  type TaskPriority,
  type TaskResponse,
  type TaskStatus,
  type TaskUpdateRequest,
} from '../types'
import { DeleteTaskDialog } from './DeleteTaskDialog'
import { CategoryBadge, OverdueBadge, PriorityBadge, StatusBadge } from './TaskBadges'
import { TaskDetailDialog } from './TaskDetailDialog'
import { TaskFormDialog } from './TaskFormDialog'

const PAGE_SIZE = 10

function formatDeadline(iso: string | null): string {
  if (!iso) return 'Không có hạn chót'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function toIsoBoundary(localInput: string): string | undefined {
  const trimmed = localInput.trim()
  if (!trimmed) return undefined
  const parsed = new Date(trimmed)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

export function TasksPage() {
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('')
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | ''>('')
  const [deadlineFromLocal, setDeadlineFromLocal] = useState<string>('')
  const [deadlineToLocal, setDeadlineToLocal] = useState<string>('')
  const [page, setPage] = useState<number>(0)

  const [pageData, setPageData] = useState<PageResponse<TaskResponse> | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyTaskIds, setBusyTaskIds] = useState<ReadonlySet<number>>(() => new Set())

  const [formModal, setFormModal] = useState<
    | { open: false }
    | { open: true; mode: 'create' | 'edit'; task: TaskResponse | null; key: number }
  >({ open: false })
  const [detailTask, setDetailTask] = useState<TaskResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<TaskResponse | null>(null)
  const [isDeleting, setIsDeleting] = useState<boolean>(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const requestSeqRef = useRef<number>(0)
  const modalCounterRef = useRef<number>(0)

  const hasInvalidDateRange =
    deadlineFromLocal.trim() !== '' &&
    deadlineToLocal.trim() !== '' &&
    new Date(deadlineFromLocal) > new Date(deadlineToLocal)

  const hasActiveFilters =
    statusFilter !== '' ||
    priorityFilter !== '' ||
    deadlineFromLocal.trim() !== '' ||
    deadlineToLocal.trim() !== ''

  const fetchTasks = useCallback(async () => {
    if (hasInvalidDateRange) {
      setIsLoading(false)
      setLoadError('Mốc hạn chót bắt đầu phải trước hoặc bằng mốc kết thúc.')
      return
    }

    const seq = ++requestSeqRef.current
    setIsLoading(true)
    setLoadError(null)

    try {
      const response = await listTasks({
        status: statusFilter || undefined,
        priority: priorityFilter || undefined,
        deadlineFrom: toIsoBoundary(deadlineFromLocal),
        deadlineTo: toIsoBoundary(deadlineToLocal),
        page,
        size: PAGE_SIZE,
      })
      if (seq !== requestSeqRef.current) return
      setPageData(response)
      setDetailTask((prev) =>
        prev ? (response.content.find((item) => item.id === prev.id) ?? prev) : null,
      )
    } catch (error) {
      if (seq !== requestSeqRef.current) return
      setLoadError(taskErrorMessageOf(error))
    } finally {
      if (seq === requestSeqRef.current) {
        setIsLoading(false)
      }
    }
  }, [hasInvalidDateRange, statusFilter, priorityFilter, deadlineFromLocal, deadlineToLocal, page])

  useEffect(() => {
    void Promise.resolve().then(() => {
      void fetchTasks()
    })
  }, [fetchTasks])

  useEffect(() => {
    function onStorage() {
      setFormModal({ open: false })
      setDetailTask(null)
      setDeleteTarget(null)
      void fetchTasks()
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [fetchTasks])

  function openCreateModal() {
    modalCounterRef.current += 1
    setActionError(null)
    setFormModal({ open: true, mode: 'create', task: null, key: modalCounterRef.current })
  }

  function openEditModal(task: TaskResponse) {
    modalCounterRef.current += 1
    setActionError(null)
    setDetailTask(null)
    setFormModal({ open: true, mode: 'edit', task, key: modalCounterRef.current })
  }

  function resetFilters() {
    setStatusFilter('')
    setPriorityFilter('')
    setDeadlineFromLocal('')
    setDeadlineToLocal('')
    setPage(0)
    setActionError(null)
  }

  async function handleCreateTask(payload: TaskCreateRequest) {
    await createTask(payload)
    setPage(0)
    await fetchTasks()
  }

  async function handleUpdateTask(taskId: number, payload: TaskUpdateRequest) {
    const updated = await updateTask(taskId, payload)
    setPageData((prev) =>
      prev
        ? {
            ...prev,
            content: prev.content.map((item) => (item.id === updated.id ? updated : item)),
          }
        : prev,
    )
    await fetchTasks()
  }

  async function handleReloadLatestForForm(taskId: number) {
    const latest = await getTask(taskId)
    modalCounterRef.current += 1
    setFormModal({ open: true, mode: 'edit', task: latest, key: modalCounterRef.current })
    setPageData((prev) =>
      prev
        ? {
            ...prev,
            content: prev.content.map((item) => (item.id === latest.id ? latest : item)),
          }
        : prev,
    )
  }

  async function handleQuickStatusChange(task: TaskResponse, nextStatus: TaskStatus) {
    setActionError(null)
    setBusyTaskIds((prev) => new Set(prev).add(task.id))
    try {
      const updated = await changeTaskStatus(task.id, {
        status: nextStatus,
        version: task.version,
      })
      setPageData((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((item) => (item.id === updated.id ? updated : item)),
            }
          : prev,
      )
      setDetailTask((prev) => (prev && prev.id === updated.id ? updated : prev))
      await fetchTasks()
    } catch (error) {
      setActionError(taskErrorMessageOf(error))
    } finally {
      setBusyTaskIds((prev) => {
        const next = new Set(prev)
        next.delete(task.id)
        return next
      })
    }
  }

  async function handleConfirmDelete(task: TaskResponse) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteTask(task.id)
      setDeleteTarget(null)
      setDetailTask((prev) => (prev && prev.id === task.id ? null : prev))
      const remainingOnPage = (pageData?.content.length ?? 1) - 1
      if (remainingOnPage <= 0 && page > 0) {
        setPage((p) => Math.max(0, p - 1))
      } else {
        await fetchTasks()
      }
    } catch (error) {
      setDeleteError(taskErrorMessageOf(error))
    } finally {
      setIsDeleting(false)
    }
  }

  const tasks = pageData?.content ?? []
  const totalElements = pageData?.totalElements ?? 0
  const totalPages = pageData?.totalPages ?? 0

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Công việc cá nhân</h1>
          <p className="text-sm text-muted-foreground">
            Quản lý danh sách công việc, mức độ ưu tiên và hạn chót để lên lịch thông minh.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={isLoading}
            onClick={() => void fetchTasks()}
          >
            <RefreshCw className={cn('h-4 w-4', isLoading && 'animate-spin')} />
            Làm mới
          </Button>
          <Button type="button" size="sm" onClick={openCreateModal}>
            <Plus className="h-4 w-4" />
            Tạo công việc
          </Button>
        </div>
      </div>

      {/* Filters */}
      <section
        aria-label="Bộ lọc công việc"
        className="rounded-xl border border-border bg-card p-4 shadow-xs"
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-1.5">
            <Label htmlFor="filter-task-status">Trạng thái</Label>
            <select
              id="filter-task-status"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as TaskStatus | '')
                setPage(0)
              }}
              className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">Tất cả trạng thái</option>
              {TASK_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="filter-task-priority">Mức độ ưu tiên</Label>
            <select
              id="filter-task-priority"
              value={priorityFilter}
              onChange={(e) => {
                setPriorityFilter(e.target.value as TaskPriority | '')
                setPage(0)
              }}
              className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">Tất cả mức ưu tiên</option>
              {TASK_PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {PRIORITY_LABELS[priority]}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="filter-deadline-from">Hạn chót từ</Label>
            <Input
              id="filter-deadline-from"
              type="datetime-local"
              value={deadlineFromLocal}
              onChange={(e) => {
                setDeadlineFromLocal(e.target.value)
                setPage(0)
              }}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="filter-deadline-to">Hạn chót đến</Label>
            <Input
              id="filter-deadline-to"
              type="datetime-local"
              value={deadlineToLocal}
              onChange={(e) => {
                setDeadlineToLocal(e.target.value)
                setPage(0)
              }}
            />
          </div>
        </div>

        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-border pt-3 text-xs text-muted-foreground">
          <span>
            {pageData
              ? `Hiển thị ${tasks.length} / ${totalElements} công việc`
              : 'Đang chuẩn bị danh sách...'}
          </span>
          {hasActiveFilters && (
            <Button type="button" variant="ghost" size="sm" onClick={resetFilters}>
              <FilterX className="h-3.5 w-3.5" />
              Đặt lại bộ lọc
            </Button>
          )}
        </div>
      </section>

      {/* Action-level Error Alert (e.g. 409 STALE_VERSION on quick status change) */}
      {actionError && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          <div className="flex items-start gap-2">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>{actionError}</span>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setActionError(null)
              void fetchTasks()
            }}
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Tải lại danh sách
          </Button>
        </div>
      )}

      {/* Main Content States: Loading / Error / Empty / Success */}
      {isLoading && pageData === null ? (
        <div className="flex h-64 flex-col items-center justify-center gap-3 rounded-xl border border-border bg-card p-6 text-center">
          <RefreshCw className="h-6 w-6 animate-spin text-primary" />
          <p className="text-sm font-medium text-muted-foreground">
            Đang tải danh sách công việc...
          </p>
        </div>
      ) : loadError ? (
        <div
          role="alert"
          className="flex flex-col items-center justify-center gap-3 rounded-xl border border-destructive/40 bg-destructive/5 p-8 text-center"
        >
          <AlertCircle className="h-8 w-8 text-destructive" />
          <div className="space-y-1">
            <p className="font-semibold text-destructive">Không thể tải danh sách công việc</p>
            <p className="text-sm text-muted-foreground">{loadError}</p>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={() => void fetchTasks()}>
            <RefreshCw className="h-4 w-4" />
            Thử lại
          </Button>
        </div>
      ) : tasks.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border bg-card p-10 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <ListTodo className="h-6 w-6" />
          </div>
          <div className="space-y-1">
            <h2 className="text-base font-semibold text-foreground">
              {hasActiveFilters
                ? 'Không có công việc phù hợp với bộ lọc'
                : 'Bạn chưa có công việc cá nhân nào'}
            </h2>
            <p className="max-w-md text-sm text-muted-foreground">
              {hasActiveFilters
                ? 'Thử thay đổi điều kiện lọc hoặc đặt lại bộ lọc để xem tất cả công việc.'
                : 'Tạo công việc đầu tiên cùng thời lượng ước tính và hạn chót để hệ thống sắp xếp lịch hợp lý.'}
            </p>
          </div>
          {hasActiveFilters ? (
            <Button type="button" variant="outline" size="sm" onClick={resetFilters}>
              <FilterX className="h-4 w-4" />
              Đặt lại bộ lọc
            </Button>
          ) : (
            <Button type="button" size="sm" onClick={openCreateModal}>
              <Plus className="h-4 w-4" />
              Tạo công việc đầu tiên
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => {
            const isTaskBusy = busyTaskIds.has(task.id)
            return (
              <article
                key={task.id}
                className={cn(
                  'rounded-xl border bg-card p-4 shadow-xs transition-colors',
                  task.overdue ? 'border-destructive/50' : 'border-border',
                )}
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  {/* Left: Info */}
                  <div className="min-w-0 flex-1 space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setDetailTask(task)}
                        className="text-left text-base font-semibold text-foreground hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      >
                        {task.title}
                      </button>
                      <OverdueBadge overdue={task.overdue} />
                      <PriorityBadge priority={task.priority} />
                      <CategoryBadge category={task.category} />
                      <StatusBadge status={task.status} />
                    </div>

                    {task.description.trim() && (
                      <p className="line-clamp-2 text-sm text-muted-foreground">
                        {task.description}
                      </p>
                    )}

                    <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 pt-0.5 text-xs text-muted-foreground">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1.5',
                          task.overdue && 'font-medium text-destructive',
                        )}
                      >
                        <CalendarClock className="h-3.5 w-3.5 shrink-0" />
                        Hạn chót: {formatDeadline(task.deadline)}
                      </span>

                      <span className="inline-flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 shrink-0" />
                        Ước tính: {task.estimatedDurationMinutes} phút
                        {task.actualDurationMinutes !== null &&
                          ` • Thực tế: ${task.actualDurationMinutes} phút`}
                      </span>
                    </div>
                  </div>

                  {/* Right: Actions */}
                  <div className="flex flex-wrap items-center gap-2 lg:shrink-0">
                    <select
                      aria-label={`Trạng thái của ${task.title}`}
                      value={task.status}
                      disabled={isTaskBusy}
                      onChange={(e) => {
                        const next = e.target.value as TaskStatus
                        if (next !== task.status) {
                          void handleQuickStatusChange(task, next)
                        }
                      }}
                      className="h-9 rounded-md border border-muted-foreground/70 bg-background px-2.5 text-xs font-medium text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {TASK_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {STATUS_LABELS[status]}
                        </option>
                      ))}
                    </select>

                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isTaskBusy}
                      onClick={() => setDetailTask(task)}
                    >
                      <Eye className="h-3.5 w-3.5" />
                      Chi tiết
                    </Button>

                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isTaskBusy}
                      onClick={() => openEditModal(task)}
                    >
                      <Pencil className="h-3.5 w-3.5" />
                      Sửa
                    </Button>

                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isTaskBusy}
                      onClick={() => {
                        setDeleteError(null)
                        setDeleteTarget(task)
                      }}
                      className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      Xóa
                    </Button>
                  </div>
                </div>
              </article>
            )
          })}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 text-sm">
              <span className="text-muted-foreground">
                Trang <span className="font-semibold text-foreground">{page + 1}</span> /{' '}
                {totalPages}
              </span>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={page <= 0 || isLoading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Trang trước
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1 || isLoading}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Trang sau
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      {formModal.open && (
        <TaskFormDialog
          key={formModal.key}
          open={formModal.open}
          mode={formModal.mode}
          task={formModal.task}
          onClose={() => setFormModal({ open: false })}
          onCreate={handleCreateTask}
          onUpdate={handleUpdateTask}
          onReloadLatest={handleReloadLatestForForm}
        />
      )}

      <TaskDetailDialog
        task={detailTask}
        isBusy={detailTask !== null && busyTaskIds.has(detailTask.id)}
        onClose={() => setDetailTask(null)}
        onEdit={openEditModal}
        onDelete={(task) => {
          setDeleteError(null)
          setDeleteTarget(task)
        }}
        onStatusChange={handleQuickStatusChange}
      />

      <DeleteTaskDialog
        task={deleteTarget}
        isDeleting={isDeleting}
        errorMessage={deleteError}
        onClose={() => {
          if (!isDeleting) {
            setDeleteTarget(null)
            setDeleteError(null)
          }
        }}
        onConfirm={handleConfirmDelete}
      />
    </div>
  )
}

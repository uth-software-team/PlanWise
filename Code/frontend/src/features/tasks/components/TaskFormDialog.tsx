import { useState, type FormEvent } from 'react'
import { RefreshCw, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { isStaleVersionError, taskErrorMessageOf, taskFieldErrorsOf } from '../errors'
import {
  CATEGORY_LABELS,
  PRIORITY_LABELS,
  TASK_CATEGORIES,
  TASK_PRIORITIES,
  type TaskCategory,
  type TaskCreateRequest,
  type TaskPriority,
  type TaskResponse,
  type TaskUpdateRequest,
} from '../types'

interface TaskFormDialogProps {
  open: boolean
  mode: 'create' | 'edit'
  task: TaskResponse | null
  onClose: () => void
  onCreate: (payload: TaskCreateRequest) => Promise<void>
  onUpdate: (taskId: number, payload: TaskUpdateRequest) => Promise<void>
  onReloadLatest: (taskId: number) => Promise<void>
}

function toLocalDateTimeInput(iso: string | null): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function fromLocalDateTimeInput(localValue: string): string | null {
  const trimmed = localValue.trim()
  if (!trimmed) return null
  const parsed = new Date(trimmed)
  if (Number.isNaN(parsed.getTime())) return null
  return parsed.toISOString()
}

export function TaskFormDialog({
  open,
  mode,
  task,
  onClose,
  onCreate,
  onUpdate,
  onReloadLatest,
}: TaskFormDialogProps) {
  const isEdit = mode === 'edit' && task !== null

  const [title, setTitle] = useState<string>(() => (isEdit ? task.title : ''))
  const [description, setDescription] = useState<string>(() => (isEdit ? task.description : ''))
  const [priority, setPriority] = useState<TaskPriority>(() => (isEdit ? task.priority : 'MEDIUM'))
  const [category, setCategory] = useState<TaskCategory>(() => (isEdit ? task.category : 'STUDY'))
  const [estimatedMinutes, setEstimatedMinutes] = useState<string>(() =>
    isEdit ? String(task.estimatedDurationMinutes) : '60',
  )
  const [actualMinutes, setActualMinutes] = useState<string>(() =>
    isEdit && task.actualDurationMinutes !== null ? String(task.actualDurationMinutes) : '',
  )
  const [deadlineLocal, setDeadlineLocal] = useState<string>(() =>
    isEdit ? toLocalDateTimeInput(task.deadline) : '',
  )

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [staleConflict, setStaleConflict] = useState<boolean>(false)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [isReloading, setIsReloading] = useState<boolean>(false)

  function validateForm(): boolean {
    const nextErrors: Record<string, string> = {}
    const trimmedTitle = title.trim()

    if (!trimmedTitle) {
      nextErrors.title = 'Tiêu đề công việc không được để trống.'
    } else if (trimmedTitle.length > 255) {
      nextErrors.title = 'Tiêu đề tối đa 255 ký tự.'
    }

    const est = Number(estimatedMinutes)
    if (!estimatedMinutes.trim() || !Number.isInteger(est) || est <= 0) {
      nextErrors.estimatedDurationMinutes = 'Thời lượng ước tính phải là số nguyên lớn hơn 0 phút.'
    }

    if (isEdit && actualMinutes.trim() !== '') {
      const actual = Number(actualMinutes)
      if (!Number.isInteger(actual) || actual < 0) {
        nextErrors.actualDurationMinutes = 'Thời lượng thực tế phải là số nguyên từ 0 phút trở lên.'
      }
    }

    if (deadlineLocal.trim() !== '' && fromLocalDateTimeInput(deadlineLocal) === null) {
      nextErrors.deadline = 'Hạn chót không đúng định dạng ngày giờ.'
    }

    setFieldErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (isSubmitting || isReloading) return
    setFormError(null)
    setStaleConflict(false)

    if (!validateForm()) return

    const deadlineIso = fromLocalDateTimeInput(deadlineLocal)
    const estValue = Number(estimatedMinutes)

    setIsSubmitting(true)
    try {
      if (isEdit) {
        const actualValue =
          actualMinutes.trim() === '' ? null : Number.parseInt(actualMinutes.trim(), 10)
        await onUpdate(task.id, {
          title: title.trim(),
          description,
          deadline: deadlineIso,
          priority,
          category,
          estimatedDurationMinutes: estValue,
          actualDurationMinutes: actualValue,
          version: task.version,
        })
      } else {
        await onCreate({
          title: title.trim(),
          description,
          deadline: deadlineIso,
          priority,
          category,
          estimatedDurationMinutes: estValue,
        })
      }
      onClose()
    } catch (error) {
      setFieldErrors(taskFieldErrorsOf(error))
      setFormError(taskErrorMessageOf(error))
      setStaleConflict(isStaleVersionError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleReloadClick() {
    if (!isEdit || isReloading || isSubmitting) return
    setIsReloading(true)
    setFormError(null)
    try {
      await onReloadLatest(task.id)
      setStaleConflict(false)
      setFieldErrors({})
    } catch (error) {
      setFormError(taskErrorMessageOf(error))
    } finally {
      setIsReloading(false)
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !isSubmitting && !isReloading) onClose()
      }}
      title={isEdit ? 'Chỉnh sửa công việc' : 'Tạo công việc mới'}
      description={
        isEdit
          ? `Cập nhật thông tin cho công việc #${task.id} (phiên bản v${task.version}).`
          : 'Nhập thông tin công việc cá nhân để theo dõi và xếp lịch tự động.'
      }
    >
      <div className="max-h-[75vh] overflow-y-auto pr-1">
        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4" noValidate>
          {formError && (
            <div
              role="alert"
              className="space-y-2 rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2.5 text-sm text-destructive"
            >
              <p>{formError}</p>
              {staleConflict && isEdit && (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={isReloading}
                  onClick={() => void handleReloadClick()}
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isReloading ? 'animate-spin' : ''}`} />
                  {isReloading ? 'Đang tải bản mới...' : 'Tải lại dữ liệu mới nhất'}
                </Button>
              )}
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="task-form-title">
              Tiêu đề <span className="text-destructive">*</span>
            </Label>
            <Input
              id="task-form-title"
              value={title}
              maxLength={255}
              placeholder="Ví dụ: Ôn tập Mạng máy tính"
              aria-invalid={fieldErrors.title ? true : undefined}
              onChange={(e) => setTitle(e.target.value)}
            />
            {fieldErrors.title && <p className="text-sm text-destructive">{fieldErrors.title}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="task-form-description">Mô tả</Label>
            <textarea
              id="task-form-description"
              rows={3}
              value={description}
              placeholder="Ghi chú chi tiết hoặc các bước cần thực hiện..."
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md border border-muted-foreground/70 bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
            {fieldErrors.description && (
              <p className="text-sm text-destructive">{fieldErrors.description}</p>
            )}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="task-form-priority">
                Mức độ ưu tiên <span className="text-destructive">*</span>
              </Label>
              <select
                id="task-form-priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value as TaskPriority)}
                className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {TASK_PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {PRIORITY_LABELS[p]}
                  </option>
                ))}
              </select>
              {fieldErrors.priority && (
                <p className="text-sm text-destructive">{fieldErrors.priority}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="task-form-category">
                Danh mục <span className="text-destructive">*</span>
              </Label>
              <select
                id="task-form-category"
                value={category}
                onChange={(e) => setCategory(e.target.value as TaskCategory)}
                className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {TASK_CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {CATEGORY_LABELS[c]}
                  </option>
                ))}
              </select>
              {fieldErrors.category && (
                <p className="text-sm text-destructive">{fieldErrors.category}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="task-form-estimated">
                Thời lượng ước tính (phút) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="task-form-estimated"
                type="number"
                min={1}
                step={5}
                value={estimatedMinutes}
                aria-invalid={fieldErrors.estimatedDurationMinutes ? true : undefined}
                onChange={(e) => setEstimatedMinutes(e.target.value)}
              />
              {fieldErrors.estimatedDurationMinutes && (
                <p className="text-sm text-destructive">{fieldErrors.estimatedDurationMinutes}</p>
              )}
            </div>

            {isEdit && (
              <div className="space-y-1.5">
                <Label htmlFor="task-form-actual">Thời lượng thực tế (phút)</Label>
                <Input
                  id="task-form-actual"
                  type="number"
                  min={0}
                  step={5}
                  placeholder="Để trống nếu chưa ghi nhận"
                  value={actualMinutes}
                  aria-invalid={fieldErrors.actualDurationMinutes ? true : undefined}
                  onChange={(e) => setActualMinutes(e.target.value)}
                />
                {fieldErrors.actualDurationMinutes && (
                  <p className="text-sm text-destructive">{fieldErrors.actualDurationMinutes}</p>
                )}
              </div>
            )}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="task-form-deadline">Hạn chót (tùy chọn)</Label>
              {deadlineLocal && (
                <button
                  type="button"
                  onClick={() => setDeadlineLocal('')}
                  className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
                >
                  <X className="h-3.5 w-3.5" />
                  Xóa hạn chót
                </button>
              )}
            </div>
            <Input
              id="task-form-deadline"
              type="datetime-local"
              value={deadlineLocal}
              aria-invalid={fieldErrors.deadline ? true : undefined}
              onChange={(e) => setDeadlineLocal(e.target.value)}
            />
            {fieldErrors.deadline && (
              <p className="text-sm text-destructive">{fieldErrors.deadline}</p>
            )}
          </div>

          <div className="flex flex-wrap justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting || isReloading}
            >
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting || isReloading}>
              {isSubmitting ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo công việc'}
            </Button>
          </div>
        </form>
      </div>
    </Dialog>
  )
}

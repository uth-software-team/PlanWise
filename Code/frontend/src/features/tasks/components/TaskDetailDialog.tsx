import { CalendarClock, Clock, Pencil, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { STATUS_LABELS, TASK_STATUSES, type TaskResponse, type TaskStatus } from '../types'
import { CategoryBadge, OverdueBadge, PriorityBadge, StatusBadge } from './TaskBadges'

interface TaskDetailDialogProps {
  task: TaskResponse | null
  isBusy: boolean
  onClose: () => void
  onEdit: (task: TaskResponse) => void
  onDelete: (task: TaskResponse) => void
  onStatusChange: (task: TaskResponse, nextStatus: TaskStatus) => Promise<void>
}

function formatDateTime(iso: string | null): string {
  if (!iso) return 'Không đặt hạn chót'
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

export function TaskDetailDialog({
  task,
  isBusy,
  onClose,
  onEdit,
  onDelete,
  onStatusChange,
}: TaskDetailDialogProps) {
  return (
    <Dialog
      open={task !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      title={task ? task.title : ''}
      description={
        task ? `Mã công việc #${task.id} • Phiên bản dữ liệu v${task.version}` : undefined
      }
    >
      {task && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={task.status} />
            <PriorityBadge priority={task.priority} />
            <CategoryBadge category={task.category} />
            <OverdueBadge overdue={task.overdue} />
          </div>

          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              Mô tả
            </p>
            <p className="whitespace-pre-wrap rounded-md border border-border bg-muted/30 p-3 text-sm text-foreground">
              {task.description.trim() ? task.description : 'Không có mô tả chi tiết.'}
            </p>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="flex items-start gap-2.5 rounded-md border border-border p-3">
              <CalendarClock className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <div className="text-sm">
                <p className="text-xs text-muted-foreground">Hạn chót</p>
                <p className="font-medium text-foreground">{formatDateTime(task.deadline)}</p>
              </div>
            </div>

            <div className="flex items-start gap-2.5 rounded-md border border-border p-3">
              <Clock className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <div className="text-sm">
                <p className="text-xs text-muted-foreground">Thời lượng</p>
                <p className="font-medium text-foreground">
                  Ước tính: {task.estimatedDurationMinutes} phút
                </p>
                {task.actualDurationMinutes !== null && (
                  <p className="text-xs text-muted-foreground">
                    Thực tế: {task.actualDurationMinutes} phút
                  </p>
                )}
              </div>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="detail-task-status">Cập nhật trạng thái nhanh</Label>
            <select
              id="detail-task-status"
              value={task.status}
              disabled={isBusy}
              onChange={(e) => {
                const next = e.target.value as TaskStatus
                if (next !== task.status) {
                  void onStatusChange(task, next)
                }
              }}
              className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            >
              {TASK_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </div>

          <Separator />

          <div className="flex flex-wrap justify-between gap-2 text-xs text-muted-foreground">
            <span>Tạo lúc: {formatDateTime(task.createdAt)}</span>
            <span>Cập nhật: {formatDateTime(task.updatedAt)}</span>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2 pt-2">
            <Button
              type="button"
              variant="destructive"
              size="sm"
              disabled={isBusy}
              onClick={() => onDelete(task)}
            >
              <Trash2 className="h-4 w-4" />
              Xóa
            </Button>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" onClick={onClose}>
                Đóng
              </Button>
              <Button type="button" size="sm" disabled={isBusy} onClick={() => onEdit(task)}>
                <Pencil className="h-4 w-4" />
                Chỉnh sửa
              </Button>
            </div>
          </div>
        </div>
      )}
    </Dialog>
  )
}

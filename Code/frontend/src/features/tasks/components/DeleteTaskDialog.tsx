import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import type { TaskResponse } from '../types'

interface DeleteTaskDialogProps {
  task: TaskResponse | null
  isDeleting: boolean
  errorMessage: string | null
  onClose: () => void
  onConfirm: (task: TaskResponse) => Promise<void>
}

export function DeleteTaskDialog({
  task,
  isDeleting,
  errorMessage,
  onClose,
  onConfirm,
}: DeleteTaskDialogProps) {
  return (
    <Dialog
      open={task !== null}
      onOpenChange={(open) => {
        if (!open && !isDeleting) onClose()
      }}
      title="Xóa công việc"
      description={
        task
          ? `Bạn có chắc chắn muốn xóa công việc "${task.title}"? Công việc sẽ bị ẩn khỏi danh sách và không được đưa vào lịch tự động nữa.`
          : undefined
      }
    >
      <div className="space-y-4">
        {errorMessage && (
          <div
            role="alert"
            className="rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm text-destructive"
          >
            {errorMessage}
          </div>
        )}

        <div className="flex flex-wrap justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={isDeleting}>
            Hủy
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={!task || isDeleting}
            onClick={() => {
              if (task) void onConfirm(task)
            }}
          >
            {isDeleting ? 'Đang xóa...' : 'Xác nhận xóa'}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}

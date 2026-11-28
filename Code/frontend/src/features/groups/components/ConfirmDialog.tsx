import { useState, type ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { FormAlert } from '@/features/auth/components/FormField'

import { GroupDialog } from './GroupDialog'

type ConfirmDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  children?: ReactNode
  confirmLabel: string
  destructive?: boolean
  /** True while any group write is in flight (store `pendingWrite`). */
  busy: boolean
  /**
   * Runs the action. Returns null when it succeeded (the dialog closes) or the message to show (the dialog stays
   * open so the user can read it after the group was reloaded). `undefined` = dropped result: close silently.
   */
  onConfirm: () => Promise<string | null | undefined>
  focusFallback?: () => HTMLElement | null
}

/** Confirmation for one group action (remove a member, transfer leadership). */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  children,
  confirmLabel,
  destructive = false,
  busy,
  onConfirm,
  focusFallback,
}: ConfirmDialogProps) {
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(next: boolean) {
    if (!next) setError(null)
    onOpenChange(next)
  }

  async function confirm() {
    setError(null)
    const message = await onConfirm()
    if (message) setError(message)
    else handleOpenChange(false)
  }

  return (
    <GroupDialog
      open={open}
      onOpenChange={handleOpenChange}
      title={title}
      focusFallback={focusFallback}
    >
      <div className="space-y-4">
        {children}
        <FormAlert message={error} />
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            className="h-11"
            onClick={() => handleOpenChange(false)}
          >
            Hủy
          </Button>
          <Button
            type="button"
            variant={destructive ? 'destructive' : 'default'}
            // The destructive variant's focus ring is below 3:1; use the app's solid ring.
            className="h-11 focus-visible:ring-ring"
            disabled={busy}
            onClick={() => void confirm()}
          >
            {busy ? 'Đang xử lý…' : confirmLabel}
          </Button>
        </div>
      </div>
    </GroupDialog>
  )
}

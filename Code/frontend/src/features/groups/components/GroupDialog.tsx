import { useRef, type ReactNode } from 'react'
import { XIcon } from 'lucide-react'
import { Dialog as DialogPrimitive } from 'radix-ui'

type GroupDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Required: it names the dialog for screen readers (aria-labelledby). */
  title: string
  description?: string
  children: ReactNode
  /** Where focus goes on close when the button that opened the dialog is gone (e.g. its row was removed). */
  focusFallback?: () => HTMLElement | null
}

/**
 * Accessible modal for the groups feature, on Radix Dialog: focus trap, Esc and outside click close it, focus goes
 * back to the element that opened it (or to `focusFallback` when that element left the page), and the title and
 * description are announced. (The shared `ui/dialog.tsx` has none of this, so it is not used here.)
 */
export function GroupDialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  focusFallback,
}: GroupDialogProps) {
  const opener = useRef<HTMLElement | null>(null)

  // Controlled dialogs have no Radix Trigger. Radix fires this before it moves focus into the dialog, so
  // document.activeElement is still the button that opened it.
  function handleOpenAutoFocus() {
    opener.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
  }

  function handleCloseAutoFocus(event: Event) {
    event.preventDefault()
    const target = opener.current?.isConnected ? opener.current : (focusFallback?.() ?? null)
    target?.focus()
  }

  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/50 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0" />
        <DialogPrimitive.Content
          className="fixed top-1/2 left-1/2 z-50 w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-xl border border-border bg-background p-6 shadow-lg data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0"
          onOpenAutoFocus={handleOpenAutoFocus}
          onCloseAutoFocus={handleCloseAutoFocus}
          {...(description ? {} : { 'aria-describedby': undefined })}
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <DialogPrimitive.Title className="text-lg font-semibold text-foreground">
                {title}
              </DialogPrimitive.Title>
              {description && (
                <DialogPrimitive.Description className="mt-1 text-sm text-muted-foreground">
                  {description}
                </DialogPrimitive.Description>
              )}
            </div>
            <DialogPrimitive.Close
              className="inline-flex size-11 shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:outline-none"
              aria-label="Đóng"
            >
              <XIcon className="size-5" aria-hidden="true" />
            </DialogPrimitive.Close>
          </div>
          <div className="mt-4">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}

import type { ComponentProps } from 'react'

import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

type FormFieldProps = ComponentProps<typeof Input> & {
  id: string
  label: string
  hint?: string
  error?: string
}

/** Label + input + optional hint and error, wired with aria-describedby / aria-invalid. */
export function FormField({ id, label, hint, error, className, ...inputProps }: FormFieldProps) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined

  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn('h-11', className)}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-destructive">
          {error}
        </p>
      )}
      {hint && (
        <p id={hintId} className="text-sm text-muted-foreground">
          {hint}
        </p>
      )}
    </div>
  )
}

/** Error for the whole form; announced by screen readers when it appears. */
export function FormAlert({ message }: { message: string | null }) {
  return (
    <div role="alert" aria-live="assertive">
      {message && (
        <p className="rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          {message}
        </p>
      )}
    </div>
  )
}

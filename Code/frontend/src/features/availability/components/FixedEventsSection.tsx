import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  AlertCircle,
  AlertTriangle,
  Calendar,
  Clock,
  Pencil,
  Plus,
  RefreshCw,
  Repeat,
  Trash2,
} from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  createFixedEvent,
  deleteFixedEvent,
  listFixedEvents,
  updateFixedEvent,
} from '../api/availabilityApi'
import { availabilityErrorMessageOf, availabilityFieldErrorsOf } from '../errors'
import {
  DAY_OF_WEEK_OPTIONS,
  RECURRENCE_LABELS,
  getDayOfWeekLabel,
  type FixedEventRequest,
  type FixedEventResponse,
  type RecurrenceType,
} from '../types'

function formatTimeDisplay(timeStr: string | null): string {
  return timeStr ? timeStr.slice(0, 5) : ''
}

function normalizeTimePayload(timeInput: string): string {
  const trimmed = timeInput.trim()
  return trimmed.length === 5 ? `${trimmed}:00` : trimmed
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

function formatInstantDisplay(iso: string | null): string {
  if (!iso) return ''
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

export function FixedEventsSection() {
  const [events, setEvents] = useState<FixedEventResponse[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [sessionAffectedWarning, setSessionAffectedWarning] = useState<string | null>(null)

  // Form dialog state
  const [modalOpen, setModalOpen] = useState<boolean>(false)
  const [editingEvent, setEditingEvent] = useState<FixedEventResponse | null>(null)

  const [title, setTitle] = useState<string>('')
  const [note, setNote] = useState<string>('')
  const [recurrenceType, setRecurrenceType] = useState<RecurrenceType>('WEEKLY')
  const [dayOfWeek, setDayOfWeek] = useState<number>(1)
  const [startTime, setStartTime] = useState<string>('09:00')
  const [endTime, setEndTime] = useState<string>('11:00')
  const [startAtLocal, setStartAtLocal] = useState<string>('')
  const [endAtLocal, setEndAtLocal] = useState<string>('')
  const [effectiveFrom, setEffectiveFrom] = useState<string>('')
  const [effectiveUntil, setEffectiveUntil] = useState<string>('')

  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [formSubmitError, setFormSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)

  // Delete dialog state
  const [deleteTarget, setDeleteTarget] = useState<FixedEventResponse | null>(null)
  const [isDeleting, setIsDeleting] = useState<boolean>(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const fetchEvents = useCallback(async () => {
    setIsLoading(true)
    setLoadError(null)
    try {
      const data = await listFixedEvents()
      setEvents(data)
    } catch (error) {
      setLoadError(availabilityErrorMessageOf(error))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void Promise.resolve().then(() => {
      void fetchEvents()
    })
  }, [fetchEvents])

  function openCreateModal() {
    setEditingEvent(null)
    setTitle('')
    setNote('')
    setRecurrenceType('WEEKLY')
    setDayOfWeek(1)
    setStartTime('09:00')
    setEndTime('11:00')
    setStartAtLocal('')
    setEndAtLocal('')
    setEffectiveFrom('')
    setEffectiveUntil('')
    setFormErrors({})
    setFormSubmitError(null)
    setModalOpen(true)
  }

  function openEditModal(event: FixedEventResponse) {
    setEditingEvent(event)
    setTitle(event.title)
    setNote(event.note ?? '')
    setRecurrenceType(event.recurrenceType)
    setDayOfWeek(event.dayOfWeek ?? 1)
    setStartTime(formatTimeDisplay(event.startTime) || '09:00')
    setEndTime(formatTimeDisplay(event.endTime) || '11:00')
    setStartAtLocal(toLocalDateTimeInput(event.startAt))
    setEndAtLocal(toLocalDateTimeInput(event.endAt))
    setEffectiveFrom(event.effectiveFrom ?? '')
    setEffectiveUntil(event.effectiveUntil ?? '')
    setFormErrors({})
    setFormSubmitError(null)
    setModalOpen(true)
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {}
    const trimmedTitle = title.trim()
    if (!trimmedTitle) {
      errors.title = 'Tiêu đề sự kiện không được để trống.'
    } else if (trimmedTitle.length > 255) {
      errors.title = 'Tiêu đề tối đa 255 ký tự.'
    }

    if (recurrenceType === 'WEEKLY') {
      if (!startTime || !endTime) {
        errors.timeRange = 'Vui lòng nhập giờ bắt đầu và giờ kết thúc.'
      } else if (startTime >= endTime) {
        errors.timeRange = 'Giờ kết thúc phải sau giờ bắt đầu.'
      }
    } else {
      const startIso = fromLocalDateTimeInput(startAtLocal)
      const endIso = fromLocalDateTimeInput(endAtLocal)
      if (!startIso || !endIso) {
        errors.instantRange = 'Vui lòng nhập đầy đủ ngày giờ bắt đầu và kết thúc.'
      } else if (new Date(startIso).getTime() >= new Date(endIso).getTime()) {
        errors.instantRange = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.'
      }
    }

    if (effectiveFrom && effectiveUntil && effectiveFrom > effectiveUntil) {
      errors.effectiveRange = 'Ngày kết thúc hiệu lực không được trước ngày bắt đầu.'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleFormSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (isSubmitting) return
    setFormSubmitError(null)
    if (!validateForm()) return

    const trimmedNote = note.trim()
    const payload: FixedEventRequest =
      recurrenceType === 'WEEKLY'
        ? {
            title: title.trim(),
            note: trimmedNote ? trimmedNote : null,
            recurrenceType: 'WEEKLY',
            dayOfWeek,
            startTime: normalizeTimePayload(startTime),
            endTime: normalizeTimePayload(endTime),
            startAt: null,
            endAt: null,
            effectiveFrom: effectiveFrom || null,
            effectiveUntil: effectiveUntil || null,
          }
        : {
            title: title.trim(),
            note: trimmedNote ? trimmedNote : null,
            recurrenceType: 'ONCE',
            dayOfWeek: null,
            startTime: null,
            endTime: null,
            startAt: fromLocalDateTimeInput(startAtLocal),
            endAt: fromLocalDateTimeInput(endAtLocal),
            effectiveFrom: effectiveFrom || null,
            effectiveUntil: effectiveUntil || null,
          }

    setIsSubmitting(true)
    try {
      const saved =
        editingEvent !== null
          ? await updateFixedEvent(editingEvent.id, payload)
          : await createFixedEvent(payload)

      if (editingEvent !== null) {
        setEvents((prev) => prev.map((item) => (item.id === saved.id ? saved : item)))
      } else {
        setEvents((prev) => [...prev, saved])
      }

      if (saved.warnings.includes('SESSION_AFFECTED')) {
        setSessionAffectedWarning(
          `Lưu ý: Sự kiện cố định "${saved.title}" bị trùng giờ với một hoặc nhiều phiên học/làm việc đã xác nhận trên lịch.`,
        )
      } else {
        setSessionAffectedWarning(null)
      }

      setModalOpen(false)
    } catch (error) {
      setFormErrors(availabilityFieldErrorsOf(error))
      setFormSubmitError(availabilityErrorMessageOf(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleConfirmDelete() {
    if (!deleteTarget || isDeleting) return
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteFixedEvent(deleteTarget.id)
      setEvents((prev) => prev.filter((item) => item.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch (error) {
      setDeleteError(availabilityErrorMessageOf(error))
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Lịch cố định</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Các lịch học, lịch họp cố định hàng tuần hoặc sự kiện cá nhân một lần. Hệ thống sẽ tránh
            các khung giờ này khi tạo lịch.
          </p>
        </div>

        <Button type="button" size="sm" onClick={openCreateModal}>
          <Plus className="h-4 w-4" />
          Thêm lịch cố định
        </Button>
      </div>

      {sessionAffectedWarning && (
        <div
          role="status"
          className="mt-4 flex items-start justify-between gap-3 rounded-lg border border-amber-300 bg-amber-50 px-3.5 py-2.5 text-sm text-amber-900"
        >
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
            <span>{sessionAffectedWarning}</span>
          </div>
          <button
            type="button"
            onClick={() => setSessionAffectedWarning(null)}
            className="text-xs font-medium text-amber-800 hover:underline"
          >
            Đóng
          </button>
        </div>
      )}

      <div className="mt-5">
        {isLoading ? (
          <div
            role="status"
            className="flex items-center gap-2 rounded-lg border border-border bg-muted/30 p-4 text-sm text-muted-foreground"
          >
            <RefreshCw className="h-4 w-4 animate-spin text-primary" />
            <span>Đang tải danh sách lịch cố định...</span>
          </div>
        ) : loadError ? (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm"
          >
            <span className="text-destructive">{loadError}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void fetchEvents()}>
              <RefreshCw className="h-3.5 w-3.5" />
              Thử lại
            </Button>
          </div>
        ) : events.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border bg-muted/20 p-6 text-center">
            <p className="text-sm font-medium text-foreground">Chưa có lịch cố định nào</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Thêm các buổi họp cố định trên trường hoặc lịch hẹn riêng để lịch thông minh không xếp
              chồng lên.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={openCreateModal}
            >
              <Plus className="h-3.5 w-3.5" />
              Thêm lịch cố định đầu tiên
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {events.map((event) => (
              <article
                key={event.id}
                className="flex flex-col gap-3 rounded-lg border border-border p-4 sm:flex-row sm:items-start sm:justify-between"
              >
                <div className="min-w-0 flex-1 space-y-1.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-sm font-semibold text-foreground">{event.title}</h3>
                    <Badge variant={event.recurrenceType === 'WEEKLY' ? 'proposed' : 'neutral'}>
                      {event.recurrenceType === 'WEEKLY' ? (
                        <Repeat className="h-3 w-3" />
                      ) : (
                        <Calendar className="h-3 w-3" />
                      )}
                      {RECURRENCE_LABELS[event.recurrenceType]}
                    </Badge>
                  </div>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                    {event.recurrenceType === 'WEEKLY' && event.dayOfWeek !== null ? (
                      <span className="inline-flex items-center gap-1.5 font-medium text-foreground">
                        <Clock className="h-3.5 w-3.5 text-primary" />
                        {getDayOfWeekLabel(event.dayOfWeek)}, {formatTimeDisplay(event.startTime)} –{' '}
                        {formatTimeDisplay(event.endTime)}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 font-medium text-foreground">
                        <Clock className="h-3.5 w-3.5 text-primary" />
                        {formatInstantDisplay(event.startAt)} – {formatInstantDisplay(event.endAt)}
                      </span>
                    )}

                    {(event.effectiveFrom || event.effectiveUntil) && (
                      <span>
                        Hiệu lực: {event.effectiveFrom ?? '...'} → {event.effectiveUntil ?? '...'}
                      </span>
                    )}
                  </div>

                  {event.note && event.note.trim() !== '' && (
                    <p className="text-xs text-muted-foreground">{event.note}</p>
                  )}
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => openEditModal(event)}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    Sửa
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setDeleteError(null)
                      setDeleteTarget(event)
                    }}
                    className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Xóa
                  </Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      {/* Create / Edit Modal */}
      <Dialog
        open={modalOpen}
        onOpenChange={(open) => {
          if (!open && !isSubmitting) setModalOpen(false)
        }}
        title={editingEvent ? 'Chỉnh sửa lịch cố định' : 'Thêm lịch cố định'}
        description="Thiết lập sự kiện lặp hàng tuần (WEEKLY) hoặc một lần duy nhất (ONCE)."
      >
        <div className="max-h-[75vh] overflow-y-auto pr-1">
          <form onSubmit={(e) => void handleFormSubmit(e)} className="space-y-4" noValidate>
            {formSubmitError && (
              <div
                role="alert"
                className="flex items-start gap-2 rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm text-destructive"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{formSubmitError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="fixed-event-title">
                Tiêu đề <span className="text-destructive">*</span>
              </Label>
              <Input
                id="fixed-event-title"
                value={title}
                maxLength={255}
                placeholder="Ví dụ: Họp nhóm / Khám răng"
                onChange={(e) => setTitle(e.target.value)}
              />
              {formErrors.title && <p className="text-sm text-destructive">{formErrors.title}</p>}
            </div>

            <div className="space-y-1.5">
              <Label>
                Kiểu lặp lại <span className="text-destructive">*</span>
              </Label>
              <div className="grid grid-cols-2 gap-2">
                {(['WEEKLY', 'ONCE'] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => {
                      setRecurrenceType(type)
                      setFormErrors({})
                    }}
                    className={`flex h-10 items-center justify-center gap-2 rounded-md border text-sm font-medium transition-colors cursor-pointer ${
                      recurrenceType === type
                        ? 'border-primary bg-primary text-primary-foreground'
                        : 'border-muted-foreground/60 bg-background text-foreground hover:bg-muted'
                    }`}
                  >
                    {type === 'WEEKLY' ? (
                      <Repeat className="h-4 w-4" />
                    ) : (
                      <Calendar className="h-4 w-4" />
                    )}
                    {RECURRENCE_LABELS[type]}
                  </button>
                ))}
              </div>
            </div>

            {recurrenceType === 'WEEKLY' ? (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="fixed-event-dow">
                    Thứ trong tuần <span className="text-destructive">*</span>
                  </Label>
                  <select
                    id="fixed-event-dow"
                    value={dayOfWeek}
                    onChange={(e) => setDayOfWeek(Number(e.target.value))}
                    className="h-10 w-full rounded-md border border-muted-foreground/70 bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {DAY_OF_WEEK_OPTIONS.map((d) => (
                      <option key={d.value} value={d.value}>
                        {d.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="fixed-event-start-time">
                      Giờ bắt đầu <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="fixed-event-start-time"
                      type="time"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="fixed-event-end-time">
                      Giờ kết thúc <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="fixed-event-end-time"
                      type="time"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                    />
                  </div>
                </div>
                {(formErrors.timeRange || formErrors.startTime || formErrors.endTime) && (
                  <p className="text-sm text-destructive">
                    {formErrors.timeRange ?? formErrors.startTime ?? formErrors.endTime}
                  </p>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="fixed-event-start-at">
                      Bắt đầu lúc <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="fixed-event-start-at"
                      type="datetime-local"
                      value={startAtLocal}
                      onChange={(e) => setStartAtLocal(e.target.value)}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="fixed-event-end-at">
                      Kết thúc lúc <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="fixed-event-end-at"
                      type="datetime-local"
                      value={endAtLocal}
                      onChange={(e) => setEndAtLocal(e.target.value)}
                    />
                  </div>
                </div>
                {(formErrors.instantRange || formErrors.startAt || formErrors.endAt) && (
                  <p className="text-sm text-destructive">
                    {formErrors.instantRange ?? formErrors.startAt ?? formErrors.endAt}
                  </p>
                )}
              </div>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="fixed-event-eff-from">Hiệu lực từ ngày (tùy chọn)</Label>
                <Input
                  id="fixed-event-eff-from"
                  type="date"
                  value={effectiveFrom}
                  onChange={(e) => setEffectiveFrom(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="fixed-event-eff-until">Đến hết ngày (tùy chọn)</Label>
                <Input
                  id="fixed-event-eff-until"
                  type="date"
                  value={effectiveUntil}
                  onChange={(e) => setEffectiveUntil(e.target.value)}
                />
              </div>
            </div>
            {formErrors.effectiveRange && (
              <p className="text-sm text-destructive">{formErrors.effectiveRange}</p>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="fixed-event-note">Ghi chú (riêng tư)</Label>
              <textarea
                id="fixed-event-note"
                rows={2}
                value={note}
                placeholder="Ghi chú cá nhân (chỉ mình bạn nhìn thấy)..."
                onChange={(e) => setNote(e.target.value)}
                className="w-full rounded-md border border-muted-foreground/70 bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>

            <div className="flex flex-wrap justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                onClick={() => setModalOpen(false)}
              >
                Hủy
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Đang lưu...' : editingEvent ? 'Lưu thay đổi' : 'Thêm lịch cố định'}
              </Button>
            </div>
          </form>
        </div>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open && !isDeleting) setDeleteTarget(null)
        }}
        title="Xóa lịch cố định"
        description={
          deleteTarget ? `Bạn có chắc muốn xóa sự kiện cố định "${deleteTarget.title}"?` : undefined
        }
      >
        <div className="space-y-4">
          {deleteError && (
            <div
              role="alert"
              className="rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm text-destructive"
            >
              {deleteError}
            </div>
          )}
          <div className="flex flex-wrap justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={isDeleting}
              onClick={() => setDeleteTarget(null)}
            >
              Hủy
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={isDeleting}
              onClick={() => void handleConfirmDelete()}
            >
              {isDeleting ? 'Đang xóa...' : 'Xác nhận xóa'}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  )
}

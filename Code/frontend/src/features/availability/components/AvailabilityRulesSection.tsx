import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { AlertCircle, Clock, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'
import {
  createAvailabilityRule,
  deleteAvailabilityRule,
  listAvailabilityRules,
  updateAvailabilityRule,
} from '../api/availabilityApi'
import { availabilityErrorMessageOf, availabilityFieldErrorsOf } from '../errors'
import { DAY_OF_WEEK_OPTIONS, getDayOfWeekLabel, type AvailabilityRuleResponse } from '../types'

function formatTimeDisplay(timeStr: string): string {
  return timeStr ? timeStr.slice(0, 5) : ''
}

function normalizeTimePayload(timeInput: string): string {
  const trimmed = timeInput.trim()
  if (trimmed.length === 5) return `${trimmed}:00`
  return trimmed
}

export function AvailabilityRulesSection() {
  const [rules, setRules] = useState<AvailabilityRuleResponse[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyIds, setBusyIds] = useState<Set<number>>(() => new Set())

  // Create / Edit modal state
  const [modalOpen, setModalOpen] = useState<boolean>(false)
  const [editingRule, setEditingRule] = useState<AvailabilityRuleResponse | null>(null)
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4, 5])
  const [startTime, setStartTime] = useState<string>('08:00')
  const [endTime, setEndTime] = useState<string>('17:00')
  const [active, setActive] = useState<boolean>(true)
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [formSubmitError, setFormSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)

  // Delete confirmation state
  const [deleteTarget, setDeleteTarget] = useState<AvailabilityRuleResponse | null>(null)
  const [isDeleting, setIsDeleting] = useState<boolean>(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const fetchRules = useCallback(async () => {
    setIsLoading(true)
    setLoadError(null)
    try {
      const data = await listAvailabilityRules()
      setRules(data)
    } catch (error) {
      setLoadError(availabilityErrorMessageOf(error))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void Promise.resolve().then(() => {
      void fetchRules()
    })
  }, [fetchRules])

  function openCreateModal() {
    setEditingRule(null)
    setSelectedDays([1, 2, 3, 4, 5])
    setStartTime('08:00')
    setEndTime('17:00')
    setActive(true)
    setFormErrors({})
    setFormSubmitError(null)
    setModalOpen(true)
  }

  function openEditModal(rule: AvailabilityRuleResponse) {
    setEditingRule(rule)
    setSelectedDays([rule.dayOfWeek])
    setStartTime(formatTimeDisplay(rule.startTime))
    setEndTime(formatTimeDisplay(rule.endTime))
    setActive(rule.active)
    setFormErrors({})
    setFormSubmitError(null)
    setModalOpen(true)
  }

  function toggleDaySelection(day: number) {
    if (editingRule !== null) {
      setSelectedDays([day])
      return
    }
    setSelectedDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day].sort((a, b) => a - b),
    )
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {}
    if (selectedDays.length === 0) {
      errors.dayOfWeek = 'Vui lòng chọn ít nhất một ngày trong tuần.'
    }
    if (!startTime || !endTime) {
      errors.timeRange = 'Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc.'
    } else if (startTime >= endTime) {
      errors.timeRange = 'Giờ kết thúc phải sau giờ bắt đầu (trong cùng một ngày).'
    }
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleFormSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (isSubmitting) return
    setFormSubmitError(null)
    if (!validateForm()) return

    setIsSubmitting(true)
    try {
      const normalizedStart = normalizeTimePayload(startTime)
      const normalizedEnd = normalizeTimePayload(endTime)

      if (editingRule !== null) {
        const updated = await updateAvailabilityRule(editingRule.id, {
          dayOfWeek: selectedDays[0],
          startTime: normalizedStart,
          endTime: normalizedEnd,
          active,
        })
        setRules((prev) =>
          prev
            .map((r) => (r.id === updated.id ? updated : r))
            .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime)),
        )
      } else {
        const createdList: AvailabilityRuleResponse[] = []
        for (const day of selectedDays) {
          const created = await createAvailabilityRule({
            dayOfWeek: day,
            startTime: normalizedStart,
            endTime: normalizedEnd,
            active,
          })
          createdList.push(created)
        }
        setRules((prev) =>
          [...prev, ...createdList].sort(
            (a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime),
          ),
        )
      }
      setModalOpen(false)
    } catch (error) {
      setFormErrors(availabilityFieldErrorsOf(error))
      setFormSubmitError(availabilityErrorMessageOf(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleToggleActive(rule: AvailabilityRuleResponse) {
    if (busyIds.has(rule.id)) return
    setActionError(null)
    setBusyIds((prev) => new Set(prev).add(rule.id))
    try {
      const updated = await updateAvailabilityRule(rule.id, {
        dayOfWeek: rule.dayOfWeek,
        startTime: rule.startTime,
        endTime: rule.endTime,
        active: !rule.active,
      })
      setRules((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
    } catch (error) {
      setActionError(availabilityErrorMessageOf(error))
    } finally {
      setBusyIds((prev) => {
        const next = new Set(prev)
        next.delete(rule.id)
        return next
      })
    }
  }

  async function handleConfirmDelete() {
    if (!deleteTarget || isDeleting) return
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteAvailabilityRule(deleteTarget.id)
      setRules((prev) => prev.filter((r) => r.id !== deleteTarget.id))
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
          <h2 className="text-lg font-semibold text-foreground">
            Quy tắc thời gian rảnh hàng tuần
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Xác định các khung giờ bạn có thể học tập hoặc làm việc mỗi tuần. Hệ thống chỉ xếp lịch
            trong các quy tắc đang bật.
          </p>
        </div>

        <Button type="button" size="sm" onClick={openCreateModal}>
          <Plus className="h-4 w-4" />
          Thêm khung giờ rảnh
        </Button>
      </div>

      {actionError && (
        <div
          role="alert"
          className="mt-4 flex items-start gap-2 rounded-lg border border-destructive/40 bg-destructive/5 px-3.5 py-2.5 text-sm text-destructive"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{actionError}</span>
        </div>
      )}

      <div className="mt-5">
        {isLoading ? (
          <div
            role="status"
            className="flex items-center gap-2 rounded-lg border border-border bg-muted/30 p-4 text-sm text-muted-foreground"
          >
            <RefreshCw className="h-4 w-4 animate-spin text-primary" />
            <span>Đang tải quy tắc thời gian rảnh...</span>
          </div>
        ) : loadError ? (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm"
          >
            <span className="text-destructive">{loadError}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void fetchRules()}>
              <RefreshCw className="h-3.5 w-3.5" />
              Thử lại
            </Button>
          </div>
        ) : rules.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border bg-muted/20 p-6 text-center">
            <p className="text-sm font-medium text-foreground">
              Chưa có quy tắc thời gian rảnh nào
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Thêm khung giờ rảnh (ví dụ Thứ Hai – Thứ Sáu, 08:00 – 17:00) để tính năng tạo lịch
              thông minh có thể sắp xếp công việc.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={openCreateModal}
            >
              <Plus className="h-3.5 w-3.5" />
              Tạo quy tắc đầu tiên
            </Button>
          </div>
        ) : (
          <div className="divide-y divide-border rounded-lg border border-border">
            {rules.map((rule) => {
              const isBusy = busyIds.has(rule.id)
              return (
                <div
                  key={rule.id}
                  className={cn(
                    'flex flex-wrap items-center justify-between gap-3 px-4 py-3 transition-colors',
                    !rule.active && 'bg-muted/30 opacity-75',
                  )}
                >
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="min-w-24 text-sm font-semibold text-foreground">
                      {getDayOfWeekLabel(rule.dayOfWeek)}
                    </span>
                    <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
                      <Clock className="h-3.5 w-3.5 text-primary" />
                      {formatTimeDisplay(rule.startTime)} – {formatTimeDisplay(rule.endTime)}
                    </span>
                    <Badge variant={rule.active ? 'confirmed' : 'neutral'}>
                      {rule.active ? 'Đang bật' : 'Đang tắt'}
                    </Badge>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isBusy}
                      onClick={() => void handleToggleActive(rule)}
                    >
                      {rule.active ? 'Tắt' : 'Bật'}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isBusy}
                      onClick={() => openEditModal(rule)}
                    >
                      <Pencil className="h-3.5 w-3.5" />
                      Sửa
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isBusy}
                      onClick={() => {
                        setDeleteError(null)
                        setDeleteTarget(rule)
                      }}
                      className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      Xóa
                    </Button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Create / Edit Modal */}
      <Dialog
        open={modalOpen}
        onOpenChange={(open) => {
          if (!open && !isSubmitting) setModalOpen(false)
        }}
        title={editingRule ? 'Chỉnh sửa quy tắc thời gian rảnh' : 'Thêm quy tắc thời gian rảnh'}
        description={
          editingRule
            ? 'Cập nhật ngày trong tuần và khung giờ rảnh theo múi giờ của bạn.'
            : 'Chọn một hoặc nhiều ngày trong tuần để áp dụng cùng khung giờ rảnh.'
        }
      >
        <form onSubmit={(e) => void handleFormSubmit(e)} className="space-y-4" noValidate>
          {formSubmitError && (
            <div
              role="alert"
              className="rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm text-destructive"
            >
              {formSubmitError}
            </div>
          )}

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>
                Ngày trong tuần <span className="text-destructive">*</span>
              </Label>
              {editingRule === null && (
                <div className="flex gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => setSelectedDays([1, 2, 3, 4, 5])}
                    className="font-medium text-primary hover:underline"
                  >
                    T2 – T6
                  </button>
                  <span className="text-muted-foreground">•</span>
                  <button
                    type="button"
                    onClick={() => setSelectedDays([1, 2, 3, 4, 5, 6, 7])}
                    className="font-medium text-primary hover:underline"
                  >
                    Cả tuần
                  </button>
                </div>
              )}
            </div>

            <div className="grid grid-cols-7 gap-1.5">
              {DAY_OF_WEEK_OPTIONS.map((day) => {
                const isSelected = selectedDays.includes(day.value)
                return (
                  <button
                    key={day.value}
                    type="button"
                    aria-pressed={isSelected}
                    onClick={() => toggleDaySelection(day.value)}
                    className={cn(
                      'flex h-10 items-center justify-center rounded-md border text-xs font-semibold transition-colors cursor-pointer',
                      isSelected
                        ? 'border-primary bg-primary text-primary-foreground'
                        : 'border-muted-foreground/50 bg-background text-foreground hover:bg-muted',
                    )}
                  >
                    {day.shortLabel}
                  </button>
                )
              })}
            </div>
            {formErrors.dayOfWeek && (
              <p className="text-sm text-destructive">{formErrors.dayOfWeek}</p>
            )}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="avail-rule-start">
                Giờ bắt đầu <span className="text-destructive">*</span>
              </Label>
              <Input
                id="avail-rule-start"
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="avail-rule-end">
                Giờ kết thúc <span className="text-destructive">*</span>
              </Label>
              <Input
                id="avail-rule-end"
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

          <label className="flex items-center gap-2.5 text-sm text-foreground cursor-pointer">
            <input
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              className="h-4 w-4 rounded border-muted-foreground text-primary focus:ring-ring"
            />
            <span>Kích hoạt ngay sau khi lưu</span>
          </label>

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
              {isSubmitting ? 'Đang lưu...' : editingRule ? 'Lưu thay đổi' : 'Thêm quy tắc'}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open && !isDeleting) setDeleteTarget(null)
        }}
        title="Xóa quy tắc thời gian rảnh"
        description={
          deleteTarget
            ? `Bạn có chắc muốn xóa khung giờ rảnh ${getDayOfWeekLabel(deleteTarget.dayOfWeek)} (${formatTimeDisplay(deleteTarget.startTime)} – ${formatTimeDisplay(deleteTarget.endTime)})?`
            : undefined
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

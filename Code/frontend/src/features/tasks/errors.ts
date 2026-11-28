import { ApiError } from '@/lib/apiClient'

const FIELD_MESSAGES: Record<string, string> = {
  title: 'Tiêu đề không được để trống (tối đa 255 ký tự).',
  description: 'Mô tả không hợp lệ.',
  deadline: 'Hạn chót không đúng định dạng ngày giờ.',
  priority: 'Vui lòng chọn mức độ ưu tiên hợp lệ.',
  category: 'Vui lòng chọn danh mục hợp lệ.',
  estimatedDurationMinutes: 'Thời lượng ước tính phải lớn hơn 0 phút.',
  actualDurationMinutes: 'Thời lượng thực tế phải lớn hơn hoặc bằng 0 phút.',
  version: 'Phiên bản dữ liệu không hợp lệ.',
}

export function taskFieldErrorsOf(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {}
  const result: Record<string, string> = {}
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    result[field] = FIELD_MESSAGES[field] ?? message
  }
  return result
}

export function isStaleVersionError(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 409 || error.code === 'STALE_VERSION')
}

export function taskErrorMessageOf(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'
  }
  switch (error.code) {
    case 'STALE_VERSION':
      return 'Dữ liệu công việc đã thay đổi ở phiên khác. Vui lòng tải lại bản mới nhất trước khi lưu.'
    case 'RESOURCE_NOT_FOUND':
      return 'Không tìm thấy công việc hoặc công việc đã bị xóa.'
    case 'INVALID_INPUT':
      return 'Vui lòng kiểm tra lại các trường thông tin được đánh dấu.'
    case 'INVALID_TIME_RANGE':
      return 'Mốc hạn chót bắt đầu phải trước mốc kết thúc.'
    case 'UNAUTHENTICATED':
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
    default:
      return error.status === 409
        ? 'Dữ liệu công việc đã thay đổi ở phiên khác. Vui lòng tải lại bản mới nhất.'
        : `Đã có lỗi xảy ra (${error.status}). Vui lòng thử lại.`
  }
}

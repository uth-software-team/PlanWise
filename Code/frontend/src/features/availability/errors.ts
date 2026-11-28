import { ApiError } from '@/lib/apiClient'

export function availabilityErrorMessageOf(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'INVALID_TIME_RANGE':
        return 'Khoảng thời gian không hợp lệ. Thời điểm kết thúc phải sau thời điểm bắt đầu.'
      case 'INVALID_INPUT':
        return 'Dữ liệu nhập chưa hợp lệ. Vui lòng kiểm tra lại các trường thông tin.'
      case 'RESOURCE_NOT_FOUND':
        return 'Không tìm thấy mục này. Dữ liệu có thể đã bị xóa trước đó.'
      case 'UNAUTHENTICATED':
        return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
      default:
        return error.message || 'Đã xảy ra lỗi khi xử lý yêu cầu.'
    }
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return 'Không thể kết nối tới máy chủ. Vui lòng thử lại.'
}

export function availabilityFieldErrorsOf(error: unknown): Record<string, string> {
  if (error instanceof ApiError && error.fieldErrors) {
    return error.fieldErrors
  }
  return {}
}

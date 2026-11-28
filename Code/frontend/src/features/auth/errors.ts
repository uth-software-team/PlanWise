import { ApiError } from '@/lib/apiClient'

/** Vietnamese text for a field the backend rejected; the backend's own messages are English. */
const FIELD_MESSAGES: Record<string, string> = {
  email: 'Email không hợp lệ (tối đa 255 ký tự).',
  password: 'Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte.',
  fullName: 'Họ và tên không được để trống (tối đa 100 ký tự).',
  timezone: 'Múi giờ không hợp lệ.',
  planningStart: 'Cần nhập cả giờ bắt đầu và giờ kết thúc.',
  planningEnd: 'Giờ kết thúc phải sau giờ bắt đầu.',
}

/** Field errors of a 400, translated; unknown fields keep the backend message. */
export function fieldErrorsOf(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {}
  const result: Record<string, string> = {}
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if (field === 'planningEnd' && error.code === 'INVALID_INPUT') {
      result[field] = FIELD_MESSAGES.planningStart
    } else {
      result[field] = FIELD_MESSAGES[field] ?? message
    }
  }
  return result
}

/** One message for the form as a whole. */
export function formErrorOf(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'
  }
  switch (error.code) {
    case 'INVALID_CREDENTIALS':
      return 'Email hoặc mật khẩu không đúng.'
    case 'EMAIL_ALREADY_EXISTS':
      return 'Email này đã được dùng cho một tài khoản khác.'
    case 'INVALID_INPUT':
      return 'Vui lòng kiểm tra lại các trường được đánh dấu.'
    case 'INVALID_TIME_RANGE':
      return 'Giờ bắt đầu phải trước giờ kết thúc.'
    case 'UNAUTHENTICATED':
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
    default:
      return `Đã có lỗi xảy ra (${error.status}). Vui lòng thử lại.`
  }
}

/** Backend limits (RegisterRequest): at least 8 characters, at most 72 bytes in UTF-8 (BCrypt). */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_BYTES = 72

export function passwordProblem(password: string): string | null {
  if (password.length < PASSWORD_MIN_LENGTH) return 'Mật khẩu cần ít nhất 8 ký tự.'
  if (new TextEncoder().encode(password).length > PASSWORD_MAX_BYTES) {
    return 'Mật khẩu quá dài (tối đa 72 byte; chữ có dấu chiếm nhiều byte hơn).'
  }
  return null
}

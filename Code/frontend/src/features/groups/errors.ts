import { ApiError } from '@/lib/apiClient'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'

/** Vietnamese text for a group-form field the backend rejected (GroupRequest, MemberAddRequest). */
const FIELD_MESSAGES: Record<string, string> = {
  groupName: 'Tên nhóm không được để trống (tối đa 100 ký tự).',
  description: 'Mô tả không hợp lệ.',
  email: 'Email không hợp lệ (tối đa 255 ký tự).',
}

export function groupFieldErrorsOf(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {}
  const result: Record<string, string> = {}
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    result[field] = FIELD_MESSAGES[field] ?? message
  }
  return result
}

/** Loading the group list or one group. */
export function loadErrorOf(error: unknown): string {
  if (!(error instanceof ApiError)) return NETWORK
  switch (error.code) {
    case 'FORBIDDEN':
      return 'Bạn không phải thành viên của nhóm này.'
    case 'RESOURCE_NOT_FOUND':
      return 'Không tìm thấy nhóm.'
    default:
      return `Không tải được dữ liệu nhóm (${error.status}). Vui lòng thử lại.`
  }
}

export function createGroupErrorOf(error: unknown): string {
  if (!(error instanceof ApiError)) return NETWORK
  if (error.code === 'INVALID_INPUT') return 'Vui lòng kiểm tra lại các trường được đánh dấu.'
  return `Không tạo được nhóm (${error.status}). Vui lòng thử lại.`
}

/** Adding a member by email. The group is reloaded for 403/404/409 before this is shown. */
export function addMemberErrorOf(error: unknown): string {
  if (!(error instanceof ApiError)) return NETWORK
  switch (error.code) {
    case 'INVALID_INPUT':
      return 'Email không hợp lệ.'
    case 'RESOURCE_NOT_FOUND':
      return 'Không tìm thấy tài khoản đang hoạt động với email này.'
    case 'DATA_CONFLICT':
      return 'Người này đã là thành viên của nhóm.'
    case 'FORBIDDEN':
      return 'Bạn không còn quyền thêm thành viên. Thông tin nhóm đã được tải lại.'
    default:
      return `Không thêm được thành viên (${error.status}). Vui lòng thử lại.`
  }
}

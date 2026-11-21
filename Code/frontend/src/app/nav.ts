import {
  Bell,
  CalendarDays,
  LayoutGrid,
  ListTodo,
  Settings,
  Sparkles,
  Users,
  type LucideIcon,
} from 'lucide-react'

export type NavItem = {
  path: string
  label: string
  icon: LucideIcon
  description: string
  /** false = route exists (protected placeholder) but is hidden from MVP navigation. */
  inNav: boolean
}

/** Default page after login (TV1 decision, FE-01). */
export const DEFAULT_APP_PATH = '/tasks'

/** All main app pages, in the order of the team design. Routes are built from this list. */
const MAIN_PAGES: NavItem[] = [
  {
    path: '/overview',
    label: 'Tổng quan',
    icon: LayoutGrid,
    description: 'Tổng quan công việc và lịch trong tuần.',
    inNav: false,
  },
  {
    path: '/tasks',
    label: 'Công việc',
    icon: ListTodo,
    description: 'Theo dõi tiến độ và ưu tiên điều quan trọng nhất.',
    inNav: true,
  },
  {
    path: '/schedule',
    label: 'Lịch biểu',
    icon: CalendarDays,
    description: 'Lịch làm việc và các phiên đã lên kế hoạch.',
    inNav: true,
  },
  {
    path: '/groups',
    label: 'Nhóm',
    icon: Users,
    description: 'Nhóm, thành viên và công việc được giao.',
    inNav: true,
  },
  {
    path: '/ai',
    label: 'Trợ lý AI',
    icon: Sparkles,
    description: 'Gợi ý từ AI để bạn xem lại trước khi lưu.',
    inNav: true,
  },
  {
    path: '/notifications',
    label: 'Thông báo',
    icon: Bell,
    description: 'Thông báo về công việc, lịch và nhóm.',
    inNav: false,
  },
]

/** Pages pinned to the bottom of the sidebar. */
const SECONDARY_PAGES: NavItem[] = [
  {
    path: '/settings',
    label: 'Cài đặt',
    icon: Settings,
    description: 'Hồ sơ và thiết lập tài khoản.',
    inNav: true,
  },
]

/** Every app page, including ones hidden from navigation. Use this for routes and titles. */
export const APP_PAGES: NavItem[] = [...MAIN_PAGES, ...SECONDARY_PAGES]

/** Visible sidebar items (MVP): hidden pages keep their routes but get no link. */
export const MAIN_NAV: NavItem[] = MAIN_PAGES.filter((item) => item.inNav)
export const SECONDARY_NAV: NavItem[] = SECONDARY_PAGES.filter((item) => item.inNav)

export function findNavItem(pathname: string): NavItem | undefined {
  return APP_PAGES.find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`))
}

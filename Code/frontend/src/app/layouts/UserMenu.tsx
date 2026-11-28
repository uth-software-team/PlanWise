import { LogOut, Settings, User } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { signOut, useCurrentUser } from '@/features/auth/session'

/** Up to two initials of the full name, e.g. "Nguyễn Minh Anh" → "NA". */
function initialsOf(fullName: string | undefined): string {
  const words = (fullName ?? '').trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return ''
  const first = words[0].charAt(0)
  const last = words.length > 1 ? words[words.length - 1].charAt(0) : ''
  return `${first}${last}`.toUpperCase()
}

/** Account menu: name and email of the current user (FE-AU), settings link and logout. */
export function UserMenu() {
  const current = useCurrentUser()
  const user = current.status === 'ready' ? current.user : undefined
  const initials = initialsOf(user?.fullName)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={user?.fullName ? `Mở menu tài khoản của ${user.fullName}` : 'Mở menu tài khoản'}
        className="flex size-11 items-center justify-center rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card"
      >
        <Avatar className="size-9">
          <AvatarFallback className="bg-accent text-sm font-semibold text-accent-foreground">
            {initials || <User aria-hidden="true" className="size-4" />}
          </AvatarFallback>
        </Avatar>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          {user ? (
            <>
              <span className="truncate">{user.fullName}</span>
              <span className="truncate text-xs font-normal text-muted-foreground">
                {user.email}
              </span>
            </>
          ) : (
            'Tài khoản'
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/settings">
            <Settings aria-hidden="true" />
            Cài đặt
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={signOut}>
          <LogOut aria-hidden="true" />
          Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

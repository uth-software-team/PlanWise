import { NavLink } from 'react-router-dom'

import { MAIN_NAV, SECONDARY_NAV, type NavItem } from '@/app/nav'
import { Separator } from '@/components/ui/separator'
import { cn } from '@/lib/utils'

type SidebarNavProps = {
  /** Called after a link is chosen, e.g. to close the mobile sheet. */
  onNavigate?: () => void
}

function NavItemLink({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.path}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          'flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors',
          'outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card',
          isActive
            ? 'bg-accent text-accent-foreground'
            : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
        )
      }
    >
      <Icon aria-hidden="true" className="size-5 shrink-0" />
      <span>{item.label}</span>
    </NavLink>
  )
}

/** Sidebar links shared by the desktop sidebar and the mobile sheet. */
export function SidebarNav({ onNavigate }: SidebarNavProps) {
  return (
    <nav aria-label="Điều hướng chính" className="flex flex-1 flex-col">
      <p className="px-3 pb-2 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
        Không gian làm việc
      </p>
      <ul className="flex flex-col gap-1">
        {MAIN_NAV.map((item) => (
          <li key={item.path}>
            <NavItemLink item={item} onNavigate={onNavigate} />
          </li>
        ))}
      </ul>
      <div className="mt-auto pt-4">
        <Separator className="mb-4" />
        <ul className="flex flex-col gap-1">
          {SECONDARY_NAV.map((item) => (
            <li key={item.path}>
              <NavItemLink item={item} onNavigate={onNavigate} />
            </li>
          ))}
        </ul>
      </div>
    </nav>
  )
}

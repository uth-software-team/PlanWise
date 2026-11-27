import { useState } from 'react'
import { Menu } from 'lucide-react'
import { Link, Outlet, useLocation } from 'react-router-dom'

import { findNavItem } from '@/app/nav'
import { BrandMark } from '@/app/layouts/BrandMark'
import { SidebarNav } from '@/app/layouts/SidebarNav'
import { UserMenu } from '@/app/layouts/UserMenu'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'

/** Signed-in shell: sidebar on md+ screens, slide-out sheet on small screens. */
export function AppLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const { pathname } = useLocation()
  const current = findNavItem(pathname)

  return (
    <div className="min-h-svh bg-background text-foreground">
      <a
        href="#main-content"
        className="sr-only z-50 rounded-md bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Bỏ qua tới nội dung chính
      </a>

      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-border bg-card px-4 py-6 md:flex">
        <Link
          to="/tasks"
          aria-label="PlanWise AI, về trang Công việc"
          className="mb-8 rounded-lg px-2 outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card"
        >
          <BrandMark />
        </Link>
        <SidebarNav />
      </aside>

      <div className="flex min-h-svh flex-col md:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-card px-4 md:px-8">
          <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
            <SheetTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="size-11 md:hidden"
                aria-label="Mở menu điều hướng"
              >
                <Menu aria-hidden="true" className="size-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="flex w-72 flex-col gap-0 bg-card p-4">
              <SheetHeader className="mb-6 p-2">
                <SheetTitle>
                  <BrandMark />
                </SheetTitle>
                <SheetDescription className="sr-only">
                  Điều hướng chính của PlanWise
                </SheetDescription>
              </SheetHeader>
              <SidebarNav onNavigate={() => setMobileNavOpen(false)} />
            </SheetContent>
          </Sheet>

          <p className="truncate text-sm font-semibold text-foreground">
            {current?.label ?? 'PlanWise AI'}
          </p>

          <div className="ml-auto flex items-center gap-2">
            <UserMenu />
          </div>
        </header>

        <main id="main-content" tabIndex={-1} className="flex-1 px-4 py-8 outline-none md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

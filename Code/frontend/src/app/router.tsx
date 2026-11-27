import type { ReactNode } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { AppLayout } from '@/app/layouts/AppLayout'
import { AuthLayout } from '@/app/layouts/AuthLayout'
import { APP_PAGES, DEFAULT_APP_PATH } from '@/app/nav'
import { SchedulePage } from '@/features/schedule/components/SchedulePage'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { PlaceholderPage } from '@/routes/PlaceholderPage'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicOnlyRoute } from '@/routes/PublicOnlyRoute'

/** Feature pages that replace a placeholder; paths stay those of APP_PAGES (FE-01 rule). */
const FEATURE_PAGES: Partial<Record<string, ReactNode>> = {
  '/schedule': <SchedulePage />,
}

export const router = createBrowserRouter([
  {
    element: <PublicOnlyRoute />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          {
            path: '/login',
            element: (
              <PlaceholderPage
                eyebrow="Chào mừng trở lại"
                title="Đăng nhập"
                description="Đăng nhập để tiếp tục với kế hoạch của bạn."
                note="Form đăng nhập sẽ được làm ở FE-AU."
              />
            ),
          },
          {
            path: '/register',
            element: (
              <PlaceholderPage
                eyebrow="Bắt đầu miễn phí"
                title="Tạo tài khoản"
                description="Tạo không gian để bắt đầu sắp xếp tuần của bạn."
                note="Form đăng ký sẽ được làm ở FE-AU."
              />
            ),
          },
        ],
      },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to={DEFAULT_APP_PATH} replace /> },
          ...APP_PAGES.map((item) => ({
            path: item.path,
            element: FEATURE_PAGES[item.path] ?? (
              <PlaceholderPage
                eyebrow="Không gian cá nhân"
                title={item.label}
                description={item.description}
              />
            ),
          })),
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

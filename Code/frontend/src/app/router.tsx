import type { ReactNode } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { AppLayout } from '@/app/layouts/AppLayout'
import { AuthLayout } from '@/app/layouts/AuthLayout'
import { APP_PAGES, DEFAULT_APP_PATH } from '@/app/nav'
import { LoginPage } from '@/features/auth/components/LoginPage'
import { RegisterPage } from '@/features/auth/components/RegisterPage'
import { SettingsPage } from '@/features/auth/components/SettingsPage'
import { SchedulePage } from '@/features/schedule/components/SchedulePage'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { PlaceholderPage } from '@/routes/PlaceholderPage'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicOnlyRoute } from '@/routes/PublicOnlyRoute'

/** Feature pages that replace a placeholder; paths stay those of APP_PAGES (FE-01 rule). */
const FEATURE_PAGES: Partial<Record<string, ReactNode>> = {
  '/schedule': <SchedulePage />,
  '/settings': <SettingsPage />,
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
            element: <LoginPage />,
          },
          { path: '/register', element: <RegisterPage /> },
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

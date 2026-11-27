import { Outlet } from 'react-router-dom'

import { BrandMark } from '@/app/layouts/BrandMark'

/** Split layout for /login and /register, following the team design. */
export function AuthLayout() {
  return (
    <div className="grid min-h-svh bg-card lg:grid-cols-2">
      <aside className="hidden flex-col justify-between bg-gradient-to-br from-indigo-800 via-indigo-700 to-indigo-600 p-12 text-white lg:flex">
        <BrandMark inverted />
        <div>
          <p className="text-sm font-semibold tracking-widest text-indigo-100 uppercase">
            Lập kế hoạch có chủ đích
          </p>
          <p className="mt-4 text-4xl leading-tight font-semibold">
            Lên kế hoạch thông minh. Làm chủ thời gian.
          </p>
        </div>
        <p className="text-sm text-indigo-100">Kế hoạch rõ ràng, mỗi ngày chủ động hơn.</p>
      </aside>
      <main className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden">
            <BrandMark />
          </div>
          <Outlet />
        </div>
      </main>
    </div>
  )
}

import { Link } from 'react-router-dom'

import { DEFAULT_APP_PATH } from '@/app/nav'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 bg-background p-4 text-center">
      <p className="text-sm font-semibold text-primary">404</p>
      <h1 className="text-2xl font-semibold text-foreground">Không tìm thấy trang</h1>
      <p className="max-w-sm text-muted-foreground">
        Đường dẫn không tồn tại hoặc đã được thay đổi.
      </p>
      <Button asChild>
        <Link to={DEFAULT_APP_PATH}>Về trang Công việc</Link>
      </Button>
    </main>
  )
}

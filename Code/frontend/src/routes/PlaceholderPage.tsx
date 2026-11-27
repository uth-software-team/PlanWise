type PlaceholderPageProps = {
  eyebrow?: string
  title: string
  description: string
  note?: string
}

/** Temporary page until the owning feature task replaces it. */
export function PlaceholderPage({ eyebrow, title, description, note }: PlaceholderPageProps) {
  return (
    <section aria-labelledby="page-title" className="mx-auto w-full max-w-6xl">
      {eyebrow && (
        <p className="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
          {eyebrow}
        </p>
      )}
      <h1 id="page-title" className="mt-2 text-3xl font-semibold tracking-tight text-foreground">
        {title}
      </h1>
      <p className="mt-2 text-muted-foreground">{description}</p>
      <div className="mt-8 rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
        {note ?? 'Nội dung trang sẽ được bổ sung ở task tiếp theo.'}
      </div>
    </section>
  )
}

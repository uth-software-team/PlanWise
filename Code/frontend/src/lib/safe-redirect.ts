import { DEFAULT_APP_PATH } from '@/app/nav'

/** Pages a signed-in user can never land on; sending them there would loop back through PublicOnlyRoute. */
const PUBLIC_ONLY_PATHS: ReadonlySet<string> = new Set(['/login', '/register'])

/**
 * Turns a `?redirect=` value into an in-app path (SA-13, CWE-601). Accepts only a path that starts with a single
 * `/`, then normalises it with the URL parser (which drops tabs/newlines and turns `\` into `/`) and requires the
 * same origin afterwards. Anything else, a missing value, or a public-only page gives DEFAULT_APP_PATH.
 * Returns pathname + search + hash, so the query of the original page is kept.
 */
export function resolveRedirect(
  value: string | null | undefined,
  origin: string = window.location.origin,
): string {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return DEFAULT_APP_PATH
  }

  let url: URL
  try {
    url = new URL(value, origin)
  } catch {
    return DEFAULT_APP_PATH
  }

  if (url.origin !== origin || PUBLIC_ONLY_PATHS.has(url.pathname)) {
    return DEFAULT_APP_PATH
  }

  return `${url.pathname}${url.search}${url.hash}`
}

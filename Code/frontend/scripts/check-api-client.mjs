// FE-02 behaviour checks for src/lib/apiClient.ts + src/lib/auth-token.ts, without a test framework.
// The real modules are loaded through Vite SSR (so `@/` and import.meta.env work); storage is faked.
//
//   npm run check:api-client
//     T1–T9 with a fake fetch and fake tokens. No backend or network needed.
//   npm run check:api-client -- --base http://localhost:5173/api/v1
//     I2–I3 against a running backend through that base URL. Reads PLANWISE_CHECK_TOKEN (a valid
//     token) and PLANWISE_CHECK_EMAIL from the environment; the token is never printed.
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const ROOT = fileURLToPath(new URL('..', import.meta.url))
const KEY = 'planwise.accessToken'
const LEGACY_KEY = 'planwise_token'

// ---------- fake browser storage ----------

const store = new Map()
const fakeStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
}
Object.defineProperty(globalThis, 'localStorage', {
  value: fakeStorage,
  configurable: true,
  writable: true,
})
Object.defineProperty(globalThis, 'window', {
  value: globalThis,
  configurable: true,
  writable: true,
})
globalThis.addEventListener ??= () => {}
globalThis.removeEventListener ??= () => {}

// ---------- module loading ----------

async function startVite(baseUrl) {
  if (baseUrl === undefined) delete process.env.VITE_API_BASE_URL
  else process.env.VITE_API_BASE_URL = baseUrl
  return createServer({
    root: ROOT,
    configFile: `${ROOT}/vite.config.ts`,
    // Never read .env* files: the base URL is the default or the one passed in explicitly.
    envDir: false,
    server: { middlewareMode: true, hmr: false, watch: null },
    appType: 'custom',
    logLevel: 'error',
  })
}

/** Re-evaluates both modules, so module-load behaviour (legacy key cleanup) runs against `store`. */
async function load(server) {
  server.moduleGraph.invalidateAll()
  const api = await server.ssrLoadModule('/src/lib/apiClient.ts')
  const auth = await server.ssrLoadModule('/src/lib/auth-token.ts')
  return { api, auth }
}

// ---------- fake fetch ----------

let calls = []
let respond = () => new Response(null, { status: 500 })
const realFetch = globalThis.fetch

function useFakeFetch(handler) {
  calls = []
  respond = handler
  globalThis.fetch = async (url, init = {}) => {
    calls.push({ url: String(url), auth: new Headers(init.headers).get('Authorization') })
    return respond()
  }
}

const json = (status, body) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

// ---------- assertions ----------

const results = []

async function check(id, description, fn) {
  try {
    await fn()
    results.push({ id, ok: true })
    console.log(`PASS ${id} ${description}`)
  } catch (err) {
    results.push({ id, ok: false })
    console.log(`FAIL ${id} ${description}: ${err instanceof Error ? err.message : String(err)}`)
  }
}

function expectEqual(actual, expected, what) {
  if (actual !== expected) {
    throw new Error(`${what}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
  }
}

async function expectApiError(promise, status, code) {
  try {
    await promise
  } catch (err) {
    expectEqual(err?.name, 'ApiError', 'error type')
    expectEqual(err.status, status, 'error status')
    expectEqual(err.code, code, 'error code')
    return
  }
  throw new Error(`expected ApiError ${status} ${code}, but the request succeeded`)
}

function reset(entries = {}) {
  store.clear()
  for (const [k, v] of Object.entries(entries)) store.set(k, v)
}

// ---------- T1–T9: fake fetch, fake tokens ----------

async function unitChecks() {
  const server = await startVite(undefined)
  try {
    await check('T1', 'sends the stored token as a Bearer header', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api } = await load(server)
      useFakeFetch(() => json(200, { success: true, data: { id: 1 } }))
      const data = await api.apiRequest('/users/me')
      expectEqual(calls[0].auth, 'Bearer fake-a', 'Authorization')
      expectEqual(data.id, 1, 'data.id')
    })

    await check('T2', 'legacy key alone is never used and is removed', async () => {
      reset({ [LEGACY_KEY]: 'fake-legacy' })
      const { api } = await load(server)
      expectEqual(store.has(LEGACY_KEY), false, 'legacy key present')
      useFakeFetch(() => json(200, { success: true, data: null }))
      await api.apiRequest('/users/me')
      expectEqual(calls[0].auth, null, 'Authorization')
    })

    await check('T3', 'two keys with different accounts: the current session wins', async () => {
      reset({ [KEY]: 'fake-a', [LEGACY_KEY]: 'fake-b' })
      const { api } = await load(server)
      useFakeFetch(() => json(200, { success: true, data: null }))
      await api.apiRequest('/users/me')
      expectEqual(calls[0].auth, 'Bearer fake-a', 'Authorization')
      expectEqual(store.has(LEGACY_KEY), false, 'legacy key present')
      expectEqual(store.get(KEY), 'fake-a', 'current token')
    })

    await check('T4', 'logout clears both keys; the legacy token does not come back', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api, auth } = await load(server)
      store.set(LEGACY_KEY, 'fake-old') // e.g. written again by an old tab
      auth.clearToken()
      expectEqual(store.has(KEY), false, 'current key present')
      expectEqual(store.has(LEGACY_KEY), false, 'legacy key present')
      expectEqual(api.getAuthToken(), '', 'getAuthToken()')
      useFakeFetch(() => json(200, { success: true, data: null }))
      await api.apiRequest('/users/me')
      expectEqual(calls[0].auth, null, 'Authorization')
    })

    await check('T5', '401 (JSON body) for the current token ends the session', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api, auth } = await load(server)
      let notified = 0
      const unsubscribe = auth.subscribeToken(() => notified++)
      useFakeFetch(() =>
        json(401, { success: false, code: 'UNAUTHENTICATED', message: 'Authentication required' }),
      )
      await expectApiError(api.apiRequest('/users/me'), 401, 'UNAUTHENTICATED')
      unsubscribe()
      expectEqual(store.has(KEY), false, 'token present')
      expectEqual(notified > 0, true, 'listener notified')
    })

    await check('T6', '401 with an empty body also ends the session', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api } = await load(server)
      useFakeFetch(() => new Response(null, { status: 401 }))
      await expectApiError(api.apiRequest('/users/me'), 401, 'HTTP_ERROR')
      expectEqual(store.has(KEY), false, 'token present')
    })

    await check('T7', 'late 401 for token A after switching to B keeps B', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api, auth } = await load(server)
      let release
      const gate = new Promise((resolve) => (release = resolve))
      useFakeFetch(() =>
        gate.then(() => json(401, { success: false, code: 'UNAUTHENTICATED', message: 'expired' })),
      )
      const pending = api.apiRequest('/users/me')
      await Promise.resolve()
      expectEqual(calls[0]?.auth, 'Bearer fake-a', 'Authorization of the pending request')
      auth.setToken('fake-b')
      release()
      await expectApiError(pending, 401, 'UNAUTHENTICATED')
      expectEqual(store.get(KEY), 'fake-b', 'current token')
    })

    await check('T8', '403 keeps the session', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api } = await load(server)
      useFakeFetch(() => json(403, { success: false, code: 'FORBIDDEN', message: 'Access denied' }))
      await expectApiError(api.apiRequest('/groups/1'), 403, 'FORBIDDEN')
      expectEqual(store.get(KEY), 'fake-a', 'current token')
    })

    await check('T9a', 'default base URL is /api/v1', async () => {
      reset()
      const { api } = await load(server)
      useFakeFetch(() => json(200, { success: true, data: null }))
      await api.apiRequest('/users/me')
      expectEqual(calls[0].url, '/api/v1/users/me', 'URL')
    })
  } finally {
    await server.close()
  }

  const custom = await startVite('http://api.example.test/api/v1')
  try {
    await check('T9b', 'VITE_API_BASE_URL is honoured', async () => {
      reset()
      const { api } = await load(custom)
      useFakeFetch(() => json(200, { success: true, data: null }))
      await api.apiRequest('/users/me')
      expectEqual(calls[0].url, 'http://api.example.test/api/v1/users/me', 'URL')
    })
  } finally {
    await custom.close()
  }
}

// ---------- I2–I3: real fetch through a base URL (e.g. the Vite dev proxy) ----------

async function integrationChecks(base) {
  const token = process.env.PLANWISE_CHECK_TOKEN ?? ''
  const email = process.env.PLANWISE_CHECK_EMAIL ?? ''
  if (!token || !email)
    throw new Error('PLANWISE_CHECK_TOKEN and PLANWISE_CHECK_EMAIL are required')
  globalThis.fetch = realFetch
  const server = await startVite(base)
  try {
    await check('I2', `GET ${base}/users/me with a valid token returns the user`, async () => {
      reset({ [KEY]: token })
      const { api } = await load(server)
      const me = await api.apiRequest('/users/me')
      expectEqual(me?.email, email, 'email')
      expectEqual(store.get(KEY) === token, true, 'token kept')
    })

    await check('I3', 'a tampered token gets 401 and the session is cleared', async () => {
      const tampered = token.slice(0, -2) + (token.endsWith('AA') ? 'BB' : 'AA')
      reset({ [KEY]: tampered })
      const { api } = await load(server)
      await expectApiError(api.apiRequest('/users/me'), 401, 'UNAUTHENTICATED')
      expectEqual(store.has(KEY), false, 'token present')
    })
  } finally {
    await server.close()
  }
}

// ---------- main ----------

const baseIndex = process.argv.indexOf('--base')
if (baseIndex !== -1) {
  await integrationChecks(process.argv[baseIndex + 1])
} else {
  await unitChecks()
}
const failed = results.filter((r) => !r.ok).length
console.log(`${results.length - failed}/${results.length} passed`)
process.exitCode = failed === 0 ? 0 : 1

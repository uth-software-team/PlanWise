// FE-02 behaviour checks for src/lib/apiClient.ts + src/lib/auth-token.ts, without a test framework.
// The real modules are loaded through Vite SSR (so `@/` and import.meta.env work); storage is faked.
//
//   npm run check:api-client
//     T1–T13 with a fake fetch and fake tokens. No backend or network needed.
//     T10 field errors (ApiError.fieldErrors), T11 safe ?redirect= (SA-13), T12 shared logout,
//     T13 current-user store bound to the token (FE-AU).
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
  const session = await server.ssrLoadModule('/src/features/auth/session.ts')
  const redirect = await server.ssrLoadModule('/src/lib/safe-redirect.ts')
  return { api, auth, session, redirect }
}

// ---------- fake fetch ----------

let calls = []
let respond = () => new Response(null, { status: 500 })
const realFetch = globalThis.fetch

function useFakeFetch(handler) {
  calls = []
  respond = handler
  globalThis.fetch = async (url, init = {}) => {
    const auth = new Headers(init.headers).get('Authorization')
    calls.push({ url: String(url), auth })
    return respond({ url: String(url), auth })
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

/** Awaits a rejected request and returns the error (fails if it resolved). */
async function caught(promise) {
  try {
    await promise
  } catch (err) {
    return err
  }
  throw new Error('expected the request to fail, but it succeeded')
}

/** Lets pending promise callbacks (fetch handlers, .then) run. */
const settle = () => new Promise((resolve) => setTimeout(resolve, 0))

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

    await check(
      'T10a',
      '400 INVALID_INPUT (one planning bound missing) gives fieldErrors',
      async () => {
        reset({ [KEY]: 'fake-a' })
        const { api } = await load(server)
        useFakeFetch(() =>
          json(400, {
            success: false,
            code: 'INVALID_INPUT',
            message: 'Invalid input',
            data: { planningEnd: 'planningStart and planningEnd must be sent together' },
          }),
        )
        const err = await caught(api.apiRequest('/users/me', { method: 'PUT', body: '{}' }))
        expectEqual(err.code, 'INVALID_INPUT', 'code')
        expectEqual(
          err.fieldErrors.planningEnd,
          'planningStart and planningEnd must be sent together',
          'fieldErrors.planningEnd',
        )
        expectEqual(err.conflicts.length, 0, 'conflicts')
      },
    )

    await check(
      'T10b',
      '400 INVALID_TIME_RANGE (start >= end) gives fieldErrors.planningEnd',
      async () => {
        reset({ [KEY]: 'fake-a' })
        const { api } = await load(server)
        useFakeFetch(() =>
          json(400, {
            success: false,
            code: 'INVALID_TIME_RANGE',
            message: 'planningStart must be before planningEnd',
            data: { planningEnd: 'must be after planningStart' },
          }),
        )
        const err = await caught(api.apiRequest('/users/me', { method: 'PUT', body: '{}' }))
        expectEqual(err.code, 'INVALID_TIME_RANGE', 'code')
        expectEqual(
          err.fieldErrors.planningEnd,
          'must be after planningStart',
          'fieldErrors.planningEnd',
        )
        expectEqual(err.message, 'planningStart must be before planningEnd', 'message')
      },
    )

    await check(
      'T10c',
      '400 with null or empty data: no field errors, general message kept',
      async () => {
        reset({ [KEY]: 'fake-a' })
        const { api } = await load(server)
        for (const data of [null, {}, ['x'], { nested: { a: 'b' } }]) {
          useFakeFetch(() =>
            json(400, { success: false, code: 'INVALID_INPUT', message: 'Invalid input', data }),
          )
          const err = await caught(api.apiRequest('/users/me', { method: 'PUT', body: '{}' }))
          expectEqual(
            Object.keys(err.fieldErrors).length,
            0,
            `fieldErrors for ${JSON.stringify(data)}`,
          )
          expectEqual(err.message, 'Invalid input', 'message')
        }
      },
    )

    await check('T10d', '409 conflicts are still read; fieldErrors stays empty', async () => {
      reset({ [KEY]: 'fake-a' })
      const { api } = await load(server)
      const conflict = { sessionId: 7, code: 'SESSION_OVERLAP', message: 'overlap' }
      useFakeFetch(() =>
        json(409, {
          success: false,
          code: 'CONSTRAINT_VIOLATION',
          message: 'Conflicts',
          data: { conflicts: [conflict] },
        }),
      )
      const err = await caught(api.apiRequest('/schedules/confirm', { method: 'POST', body: '{}' }))
      expectEqual(err.conflicts.length, 1, 'conflicts')
      expectEqual(err.conflicts[0].code, 'SESSION_OVERLAP', 'conflicts[0].code')
      expectEqual(Object.keys(err.fieldErrors).length, 0, 'fieldErrors')
    })

    await check(
      'T11',
      'resolveRedirect keeps safe in-app paths, everything else goes to /tasks',
      async () => {
        reset()
        const { redirect } = await load(server)
        const origin = 'http://localhost:5173'
        const cases = [
          ['/settings', '/settings'],
          ['/settings?tab=profile#top', '/settings?tab=profile#top'],
          ['/schedule?from=2026-12-01', '/schedule?from=2026-12-01'],
          [null, '/tasks'],
          [undefined, '/tasks'],
          ['', '/tasks'],
          ['tasks', '/tasks'],
          ['//evil.com', '/tasks'],
          ['//evil.com/path', '/tasks'],
          ['/\\evil.com', '/tasks'],
          ['/\t/evil.com', '/tasks'],
          ['/\n/evil.com', '/tasks'],
          ['https://evil.com', '/tasks'],
          ['javascript:alert(1)', '/tasks'],
          ['/login', '/tasks'],
          ['/register?redirect=/x', '/tasks'],
        ]
        for (const [input, expected] of cases) {
          expectEqual(
            redirect.resolveRedirect(input, origin),
            expected,
            `resolveRedirect(${JSON.stringify(input)})`,
          )
        }
      },
    )

    await check('T12a', 'signOut ends the local session even when the network fails', async () => {
      reset({ [KEY]: 'fake-a' })
      const { session } = await load(server)
      useFakeFetch(() => Promise.reject(new TypeError('Failed to fetch')))
      session.signOut()
      expectEqual(store.has(KEY), false, 'token present right after signOut')
      await settle()
      expectEqual(calls[0]?.url, '/api/v1/auth/logout', 'logout URL')
      expectEqual(calls[0]?.auth, 'Bearer fake-a', 'logout sends the old token')
      expectEqual(store.has(KEY), false, 'token present after the failed call')
    })

    await check('T12b', 'a late 401 for the logout request keeps a newer session', async () => {
      reset({ [KEY]: 'fake-a' })
      const { session, auth } = await load(server)
      let release
      const gate = new Promise((resolve) => (release = resolve))
      useFakeFetch(() =>
        gate.then(() => json(401, { success: false, code: 'UNAUTHENTICATED', message: 'expired' })),
      )
      session.signOut()
      auth.setToken('fake-b')
      release()
      await settle()
      expectEqual(store.get(KEY), 'fake-b', 'current token')
    })

    await check(
      'T13a',
      'current user: a late GET for token A is dropped after switching to B',
      async () => {
        reset({ [KEY]: 'fake-a' })
        const { session, auth } = await load(server)
        let release
        const gate = new Promise((resolve) => (release = resolve))
        useFakeFetch(({ auth: header }) =>
          header === 'Bearer fake-a'
            ? gate.then(() =>
                json(200, { success: true, data: { id: 1, email: 'a@example.test' } }),
              )
            : json(200, { success: true, data: { id: 2, email: 'b@example.test' } }),
        )
        const unsubscribe = session.subscribeCurrentUser(() => {})
        try {
          expectEqual(session.getCurrentUserSnapshot().status, 'loading', 'status while A loads')
          auth.setToken('fake-b') // e.g. a login in another tab
          await settle()
          release()
          await settle()
          const state = session.getCurrentUserSnapshot()
          expectEqual(state.status, 'ready', 'status')
          expectEqual(state.user?.email, 'b@example.test', 'user after the late A response')
        } finally {
          unsubscribe()
        }
      },
    )

    await check(
      'T13b',
      'current user: a saved profile updates the store only for its own token',
      async () => {
        reset({ [KEY]: 'fake-b' })
        const { session } = await load(server)
        useFakeFetch(() =>
          json(200, { success: true, data: { id: 2, email: 'b@example.test', fullName: 'Old' } }),
        )
        const unsubscribe = session.subscribeCurrentUser(() => {})
        try {
          await settle()
          expectEqual(session.getCurrentUserSnapshot().user?.fullName, 'Old', 'loaded name')
          const late = session.setCurrentUser(
            { id: 1, email: 'a@example.test', fullName: 'A' },
            'fake-a',
          )
          expectEqual(late, false, 'PUT result for an ended session accepted')
          expectEqual(
            session.getCurrentUserSnapshot().user?.fullName,
            'Old',
            'name after the late PUT',
          )
          const saved = session.setCurrentUser(
            { id: 2, email: 'b@example.test', fullName: 'New' },
            'fake-b',
          )
          expectEqual(saved, true, 'PUT result for the current session accepted')
          expectEqual(session.getCurrentUserSnapshot().user?.fullName, 'New', 'name after the PUT')
        } finally {
          unsubscribe()
        }
      },
    )
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

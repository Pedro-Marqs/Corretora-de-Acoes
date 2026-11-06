import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authenticatedFetch } from './http.js'
import { clearAccessToken, startSession } from './auth-token.js'

describe('HTTP authentication preparation', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.stubGlobal('fetch', vi.fn())
    clearAccessToken()
  })

  it('adds the in-memory Bearer token to a private request', async () => {
    startSession('access-1')
    fetch.mockResolvedValue({ ok: true, status: 204 })

    await authenticatedFetch('/api/accounts/me')

    const [, options] = fetch.mock.calls[0]
    expect(options.credentials).toBe('include')
    expect(options.headers.get('Authorization')).toBe('Bearer access-1')
  })

  it('does not persist the access token in browser storage', () => {
    startSession('access-1')

    expect(window.localStorage.getItem('accessToken')).toBeNull()
    expect(window.sessionStorage.getItem('accessToken')).toBeNull()
  })

  it('preserves an explicit authorization header', async () => {
    startSession('access-1')
    fetch.mockResolvedValue({ ok: true, status: 204 })

    await authenticatedFetch('/api/test', { headers: { Authorization: 'Bearer explicit' } })

    expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer explicit')
  })

  it('does not add Bearer to authentication endpoints', async () => {
    startSession('access-1')
    fetch.mockResolvedValue({ ok: true, status: 204 })

    await authenticatedFetch('/api/auth/refresh')

    expect(fetch.mock.calls[0][1].headers.has('Authorization')).toBe(false)
  })
})


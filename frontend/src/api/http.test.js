import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authenticatedFetch } from './http.js'
import './auth.js'
import { clearAccessToken, endSession, startSession } from './auth-token.js'

function response(body, status = 200, headers = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': 'application/json', ...headers }),
    json: vi.fn().mockResolvedValue(body),
  }
}

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
    expect(options.credentials).toBe('omit')
    expect(options.headers.get('Authorization')).toBe('Bearer access-1')
  })

  it('preserva o cookie de compatibilidade somente quando não há Bearer em memória', async () => {
    fetch.mockResolvedValue({ ok: true, status: 204 })

    await authenticatedFetch('/api/accounts/me')

    const [, options] = fetch.mock.calls[0]
    expect(options.credentials).toBe('include')
    expect(options.headers.has('Authorization')).toBe(false)
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

  it('renova uma única vez e repete somente a consulta com o token novo', async () => {
    startSession('access-old')
    fetch.mockResolvedValueOnce(response({ message: 'expired' }, 401))
      .mockResolvedValueOnce(response({ token: 'csrf-refresh' }))
      .mockResolvedValueOnce(response(null, 204, { authorization: 'Bearer access-new' }))
      .mockResolvedValueOnce(response({ ok: true }))

    await expect(authenticatedFetch('/api/dashboard?period=4W')).resolves.toMatchObject({ ok: true })

    expect(fetch).toHaveBeenCalledTimes(4)
    expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer access-old')
    expect(fetch.mock.calls[2][1].headers.Authorization).toBeUndefined()
    expect(fetch.mock.calls[3][1].headers.get('Authorization')).toBe('Bearer access-new')
  })

  it('compartilha o refresh entre respostas 401 concorrentes da mesma sessão', async () => {
    startSession('access-old')
    let dashboardCalls = 0
    fetch.mockImplementation((url) => {
      if (url === '/api/dashboard') {
        dashboardCalls += 1
        return Promise.resolve(response({ message: 'expired' }, dashboardCalls <= 2 ? 401 : 200))
      }
      if (url === '/api/csrf') return Promise.resolve(response({ token: 'csrf-refresh' }))
      if (url === '/api/auth/refresh') return Promise.resolve(response(null, 204, { authorization: 'Bearer access-new' }))
      return Promise.resolve(response({ ok: true }))
    })

    await expect(Promise.all([
      authenticatedFetch('/api/dashboard'),
      authenticatedFetch('/api/dashboard'),
    ])).resolves.toHaveLength(2)

    expect(fetch.mock.calls.filter(([url]) => url === '/api/auth/refresh')).toHaveLength(1)
  })

  it('não repete automaticamente uma operação POST após 401', async () => {
    startSession('access-old')
    fetch.mockResolvedValue(response({ message: 'expired' }, 401))

    await expect(authenticatedFetch('/api/wallet/purchases', { method: 'POST', retryOnUnauthorized: false }))
      .resolves.toMatchObject({ status: 401 })

    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('não tenta renovar um Bearer explícito que não pertence à sessão em memória', async () => {
    startSession('access-current')
    fetch.mockResolvedValue(response({ message: 'invalid bearer' }, 401))

    await expect(authenticatedFetch('/api/dashboard', { headers: { Authorization: 'Bearer invalid' } }))
      .resolves.toMatchObject({ status: 401 })

    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('mantém o token ausente após refresh inválido e devolve o 401 original', async () => {
    startSession('access-old')
    fetch.mockResolvedValueOnce(response({ message: 'expired' }, 401))
      .mockResolvedValueOnce(response({ token: 'csrf-refresh' }))
      .mockResolvedValueOnce(response({ message: 'invalid refresh' }, 401))

    await expect(authenticatedFetch('/api/wallet')).resolves.toMatchObject({ status: 401 })
    expect(fetch).toHaveBeenCalledTimes(3)
    expect((await import('./auth-token.js')).getAccessToken()).toBeNull()
  })

  it('não repete uma resposta antiga depois do encerramento da sessão', async () => {
    startSession('access-old')
    let releaseCsrf
    const csrf = new Promise((resolve) => { releaseCsrf = resolve })
    fetch.mockResolvedValueOnce(response({ message: 'expired' }, 401))
      .mockReturnValueOnce(csrf)

    const request = authenticatedFetch('/api/dashboard?period=4W')
    await Promise.resolve()
    endSession()
    releaseCsrf(response({ token: 'csrf-refresh' }))
    fetch.mockResolvedValueOnce(response(null, 204, { authorization: 'Bearer access-new' }))

    await expect(request).resolves.toMatchObject({ status: 401 })
    expect(fetch.mock.calls.some(([, options]) => options?.headers?.get?.('Authorization') === 'Bearer access-new')).toBe(false)
  })
})


import { beforeEach, describe, expect, it, vi } from 'vitest'
import { adminFetch } from './admin-http.js'
import { beginAdminSession, endAdminSession, setAdminAccessTokenForGeneration } from './admin-auth-token.js'
import './admin-auth.js'

function response(status) { return { status, ok: status >= 200 && status < 300 } }

describe('admin HTTP client', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()); endAdminSession() })

  it('anexa somente o token administrativo e repete uma leitura após refresh', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce({ ...response(200), json: () => Promise.resolve({ token: 'csrf' }), headers: new Headers({ 'content-type': 'application/json' }) })
      .mockResolvedValueOnce({ ok: true, status: 204, headers: new Headers({ Authorization: 'Bearer admin-new' }) })
      .mockResolvedValueOnce(response(200))
    const result = await adminFetch('/api/admin/dashboard')
    expect(result.status).toBe(200)
    expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer admin-old')
    expect(fetch.mock.calls[3][1].headers.get('Authorization')).toBe('Bearer admin-new')
  })

  it('não repete mutações administrativas após 401', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValue(response(401))
    const result = await adminFetch('/api/admin/investors/id/status', { method: 'PATCH', retryOnUnauthorized: false })
    expect(result.status).toBe(401)
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('não anexa bearer quando não existe sessão administrativa', async () => {
    fetch.mockResolvedValue(response(200))
    await adminFetch('/api/admin/dashboard')
    expect(fetch.mock.calls[0][1].credentials).toBe('omit')
    expect(fetch.mock.calls[0][1].headers.has('Authorization')).toBe(false)
  })

  it('retorna o 401 original quando o refresh é expirado ou não devolve token', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce({ ok: true, status: 200, headers: new Headers({ 'content-type': 'application/json' }), json: () => Promise.resolve({ token: 'csrf' }) })
      .mockResolvedValueOnce({ ok: false, status: 401, headers: new Headers(), json: () => Promise.resolve({}) })
    await expect(adminFetch('/api/admin/dashboard')).resolves.toMatchObject({ status: 401 })
  })

  it('propaga falha transitória de refresh sem repetir indefinidamente', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response(401)).mockRejectedValueOnce(new TypeError('network details'))
    await expect(adminFetch('/api/admin/dashboard')).rejects.toThrow()
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('não repete leitura quando retryOnUnauthorized está desabilitado', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValue(response(401))
    await expect(adminFetch('/api/admin/dashboard', { retryOnUnauthorized: false })).resolves.toMatchObject({ status: 401 })
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAccessToken, getAccessToken, startSession } from './auth-token.js'
import { login, logout, refreshAccessToken, restoreSession } from './auth.js'

function response(body, status = 200, headers = {}) {
  return { ok: status >= 200 && status < 300, status, headers: new Headers({ ...(body ? { 'content-type': 'application/json' } : {}), ...headers }), json: vi.fn().mockResolvedValue(body) }
}

describe('auth API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()); clearAccessToken() })

  it('envia login com CSRF e credenciais do navegador', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer access-1' }))
    await expect(login({ email: 'ana@example.com', password: 'Senha@123' })).resolves.toBe('access-1')
    expect(getAccessToken()).toBe('access-1')
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/auth/login', expect.objectContaining({ method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf' } }))
  })

  it('preserva mensagem neutra e erros estruturais do login', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Credenciais inválidas.', fieldErrors: [] }, 401))
    await expect(login({})).rejects.toMatchObject({ message: 'Credenciais inválidas.', fieldErrors: {}, status: 401 })
  })

  it('envia logout com nova confirmação CSRF', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'novo-csrf' })).mockResolvedValueOnce(response(null, 204))
    startSession('access-1')
    await logout()
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/auth/logout', expect.objectContaining({ method: 'POST', credentials: 'include' }))
    expect(fetch.mock.calls[1][1].headers).not.toHaveProperty('Authorization')
    expect(getAccessToken()).toBeNull()
  })

  it('retorna mensagem segura em falha de rede', async () => {
    fetch.mockRejectedValue(new TypeError('network details'))
    await expect(login({})).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
  })
  it('replaces the access token during refresh', async () => {
    startSession('access-1')
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer access-2' }))

    await expect(refreshAccessToken()).resolves.toBe('access-2')
    expect(getAccessToken()).toBe('access-2')
    expect(fetch.mock.calls[1][1].headers).not.toHaveProperty('Authorization')
  })

  it('rejects login without a valid Bearer header', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))

    await expect(login({ email: 'ana@example.com', password: 'Senha@123' }))
      .rejects.toMatchObject({ name: 'AuthApiError', status: 204 })
    expect(getAccessToken()).toBeNull()
  })

  it('rejects login with a malformed Bearer header', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Basic not-a-bearer' }))

    await expect(login({ email: 'ana@example.com', password: 'Senha@123' }))
      .rejects.toMatchObject({ name: 'AuthApiError', status: 204 })
    expect(getAccessToken()).toBeNull()
  })

  it('rejects refresh without a Bearer header', async () => {
    startSession('access-1')
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))

    await expect(refreshAccessToken('access-1'))
      .rejects.toMatchObject({ name: 'AuthApiError', status: 204, code: 'AUTH_CONTRACT_ERROR' })
    expect(getAccessToken()).toBeNull()
  })

  it('rejects refresh with a malformed Bearer header', async () => {
    startSession('access-1')
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Basic invalid' }))

    await expect(refreshAccessToken('access-1'))
      .rejects.toMatchObject({ name: 'AuthApiError', status: 204, code: 'AUTH_CONTRACT_ERROR' })
    expect(getAccessToken()).toBeNull()
  })

  it('restores a session explicitly when no access token is in memory', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer restored' }))

    await expect(restoreSession()).resolves.toBe('restored')
    expect(getAccessToken()).toBe('restored')
  })

  it('shares one refresh promise between concurrent callers', async () => {
    let resolveRefresh
    startSession('access-1')
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf' }))
      : new Promise((resolve) => { resolveRefresh = resolve }))

    const first = refreshAccessToken('access-1')
    const second = refreshAccessToken('access-1')
    expect(first).toBe(second)
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))

    resolveRefresh(response(null, 204, { Authorization: 'Bearer access-1' }))
    await expect(Promise.all([first, second])).resolves.toEqual(['access-1', 'access-1'])
  })

  it('ignores a refresh response from session A after login creates session B', async () => {
    let resolveOldRefresh
    startSession('access-A')
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf-A' }))
      : new Promise((resolve) => { resolveOldRefresh = resolve }))

    const oldRefresh = refreshAccessToken('access-A')
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))

    fetch.mockImplementationOnce(() => Promise.resolve(response({ token: 'csrf-B' })))
      .mockImplementationOnce(() => Promise.resolve(response(null, 204, { Authorization: 'Bearer access-B' })))
    await expect(login({ email: 'new@example.com', password: 'Senha@123' })).resolves.toBe('access-B')

    resolveOldRefresh(response(null, 204, { Authorization: 'Bearer stale-A' }))
    await expect(oldRefresh).rejects.toMatchObject({ status: 401 })
    expect(getAccessToken()).toBe('access-B')
  })

  it('ignores a refresh response that arrives after logout', async () => {
    let resolveOldRefresh
    startSession('access-A')
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf-A' }))
      : new Promise((resolve) => { resolveOldRefresh = resolve }))

    const oldRefresh = refreshAccessToken('access-A')
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))

    fetch.mockImplementationOnce(() => Promise.resolve(response({ token: 'csrf-logout' })))
      .mockImplementationOnce(() => Promise.resolve(response(null, 204)))
    await logout()
    resolveOldRefresh(response(null, 204, { Authorization: 'Bearer stale-A' }))

    await expect(oldRefresh).rejects.toMatchObject({ status: 401 })
    expect(getAccessToken()).toBeNull()
  })

  it('does not let a delayed logout response clear a newer login', async () => {
    let resolveOldLogout
    startSession('access-A')
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf-A' }))
      : new Promise((resolve) => { resolveOldLogout = resolve }))

    const oldLogout = logout()
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))

    fetch.mockImplementationOnce(() => Promise.resolve(response({ token: 'csrf-B' })))
      .mockImplementationOnce(() => Promise.resolve(response(null, 204, { Authorization: 'Bearer access-B' })))
    await expect(login({ email: 'new@example.com', password: 'Senha@123' })).resolves.toBe('access-B')

    resolveOldLogout(response(null, 204))
    await oldLogout
    expect(getAccessToken()).toBe('access-B')
  })

  it('does not start refresh for a delayed 401 after logout', async () => {
    startSession('access-A')
    fetch.mockResolvedValueOnce(response({ token: 'csrf-logout' })).mockResolvedValueOnce(response(null, 204))
    await logout()
    fetch.mockClear()

    await expect(refreshAccessToken('access-A')).rejects.toMatchObject({ status: 401 })
    expect(fetch).not.toHaveBeenCalled()
  })

  it('does not refresh again for a delayed 401 from the previous token', async () => {
    startSession('access-1')
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer access-2' }))

    await refreshAccessToken('access-1')
    await expect(refreshAccessToken('access-1')).resolves.toBe('access-2')
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('does not clear a newer token when an older refresh fails', async () => {
    let rejectRefresh
    startSession('access-A')
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf' }))
      : new Promise((resolve, reject) => { rejectRefresh = reject }))

    const refresh = refreshAccessToken('access-A')
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    startSession('new-session-token')
    rejectRefresh(new TypeError('network details'))
    await expect(refresh).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
    expect(getAccessToken()).toBe('new-session-token')
  })
})

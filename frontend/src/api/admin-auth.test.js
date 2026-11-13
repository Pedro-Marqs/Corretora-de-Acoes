import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loginAdmin, logoutAdmin, refreshAdminAccessTokenForCurrentSession, restoreAdminSession } from './admin-auth.js'
import { endAdminSession, getAdminAccessToken, beginAdminSession, setAdminAccessTokenForGeneration } from './admin-auth-token.js'

function response(body, status = 200, headers = {}) {
  return { ok: status >= 200 && status < 300, status, headers: new Headers({ ...(body ? { 'content-type': 'application/json' } : {}), ...headers }), json: vi.fn().mockResolvedValue(body) }
}

describe('admin auth API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()); endAdminSession() })

  it('faz login administrativo com CSRF e mantém o access token somente em memória', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-admin' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer admin-1' }))
    await expect(loginAdmin({ email: 'admin@example.com', password: 'SenhaAdmin1!' })).resolves.toBe('admin-1')
    expect(getAdminAccessToken()).toBe('admin-1')
    expect(localStorage.getItem('admin-access-token')).toBeNull()
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/admin/auth/login', expect.objectContaining({ method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-admin' } }))
  })

  it('compartilha uma única rotação em refresh concorrente', async () => {
    beginAdminSession()
    let resolveRefresh
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf' }))
      : url === '/api/admin/auth/logout'
        ? Promise.resolve(response(null, 204))
        : new Promise((resolve) => { resolveRefresh = resolve }))
    const first = refreshAdminAccessTokenForCurrentSession()
    const second = refreshAdminAccessTokenForCurrentSession()
    expect(first).toBe(second)
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    resolveRefresh(response(null, 204, { Authorization: 'Bearer admin-2' }))
    await expect(Promise.all([first, second])).resolves.toEqual(['admin-2', 'admin-2'])
  })

  it('não reativa uma sessão depois do logout', async () => {
    beginAdminSession()
    let resolveRefresh
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf' }))
      : url === '/api/admin/auth/logout'
        ? Promise.resolve(response(null, 204))
        : new Promise((resolve) => { resolveRefresh = resolve }))
    const refresh = restoreAdminSession()
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    await logoutAdmin()
    resolveRefresh(response(null, 204, { Authorization: 'Bearer stale-admin' }))
    await expect(refresh).rejects.toMatchObject({ status: 401 })
    expect(getAdminAccessToken()).toBeNull()
  })

  it('rejeita a resposta sem Bearer', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))
    await expect(loginAdmin({ email: 'admin@example.com', password: 'SenhaAdmin1!' })).rejects.toMatchObject({ code: 'AUTH_CONTRACT_ERROR', status: 204 })
    expect(getAdminAccessToken()).toBeNull()
  })

  it('preserva erro 401 e erros de campo do login', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Credenciais inválidas.', fieldErrors: [{ field: 'email', message: 'E-mail inválido.' }] }, 401))
    await expect(loginAdmin({})).rejects.toMatchObject({ status: 401, fieldErrors: { email: ['E-mail inválido.'] } })
  })

  it('normaliza falha de CSRF ou rede durante login', async () => {
    fetch.mockRejectedValueOnce(new TypeError('network details'))
    await expect(loginAdmin({})).rejects.toMatchObject({ message: expect.stringContaining('Não foi possível conectar') })
  })

  it.each([401, 403])('limpa token quando refresh é rejeitado com %s', async (status) => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Sessão inválida.' }, status))
    await expect(refreshAdminAccessTokenForCurrentSession('admin-old')).rejects.toMatchObject({ status })
    expect(getAdminAccessToken()).toBeNull()
  })

  it('mantém token em falha transitória de refresh e evita refresh obsoleto', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockRejectedValueOnce(new TypeError('temporary network'))
    await expect(refreshAdminAccessTokenForCurrentSession('admin-old')).rejects.toMatchObject({ message: expect.stringContaining('Não foi possível conectar') })
    expect(getAdminAccessToken()).toBe('admin-old')
    fetch.mockClear()
    await expect(refreshAdminAccessTokenForCurrentSession('other-token')).resolves.toBe('admin-old')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('rejeita refresh sem Bearer e invalida login cuja geração ficou obsoleta', async () => {
    const generation = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', generation)
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))
    await expect(refreshAdminAccessTokenForCurrentSession('admin-old')).rejects.toMatchObject({ code: 'AUTH_CONTRACT_ERROR' })

    const nextGeneration = beginAdminSession()
    setAdminAccessTokenForGeneration('admin-old', nextGeneration)
    fetch.mockClear()
    let resolveRefresh
    const oldRefresh = () => {
      fetch.mockImplementation((url) => url === '/api/csrf' ? Promise.resolve(response({ token: 'csrf-old' })) : new Promise((resolve) => { resolveRefresh = resolve }))
      return refreshAdminAccessTokenForCurrentSession('admin-old')
    }
    const stale = oldRefresh()
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    fetch.mockResolvedValueOnce(response({ token: 'csrf-new' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer admin-new' }))
    await loginAdmin({ email: 'admin@example.com', password: 'SenhaAdmin1!' })
    resolveRefresh(response(null, 204, { Authorization: 'Bearer stale-admin' }))
    await expect(stale).rejects.toMatchObject({ status: 401 })
  })

  it('não instala Bearer de um login administrativo tardio após nova sessão', async () => {
    let resolveLogin
    fetch.mockImplementation((url) => url === '/api/csrf'
      ? Promise.resolve(response({ token: 'csrf-old' }))
      : new Promise((resolve) => { resolveLogin = resolve }))
    const oldLogin = loginAdmin({ email: 'old@example.com', password: 'old' })
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    fetch.mockResolvedValueOnce(response({ token: 'csrf-new' })).mockResolvedValueOnce(response(null, 204, { Authorization: 'Bearer admin-new' }))
    await loginAdmin({ email: 'new@example.com', password: 'new' })
    resolveLogin(response(null, 204, { Authorization: 'Bearer admin-old' }))
    await expect(oldLogin).rejects.toMatchObject({ status: 401 })
    expect(getAdminAccessToken()).toBe('admin-new')
  })

  it('não inicia refresh sem sessão e mantém erro de logout do servidor', async () => {
    await expect(refreshAdminAccessTokenForCurrentSession('missing')).rejects.toMatchObject({ status: 401 })
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Não autorizado.' }, 403))
    await expect(logoutAdmin()).rejects.toMatchObject({ status: 403 })
  })

  it('rejeita gravação de token em geração diferente', () => {
    const generation = beginAdminSession()
    expect(setAdminAccessTokenForGeneration('stale', generation - 1)).toBe(false)
    expect(getAdminAccessToken()).toBeNull()
  })
})

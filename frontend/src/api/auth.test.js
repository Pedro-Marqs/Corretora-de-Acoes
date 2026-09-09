import { beforeEach, describe, expect, it, vi } from 'vitest'
import { login, logout } from './auth.js'

function response(body, status = 200) {
  return { ok: status >= 200 && status < 300, status, headers: new Headers(body ? { 'content-type': 'application/json' } : {}), json: vi.fn().mockResolvedValue(body) }
}

describe('auth API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('envia login com CSRF e credenciais do navegador', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))
    await login({ email: 'ana@example.com', password: 'Senha@123' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/auth/login', expect.objectContaining({ method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf' } }))
  })

  it('preserva mensagem neutra e erros estruturais do login', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Credenciais inválidas.', fieldErrors: [] }, 401))
    await expect(login({})).rejects.toMatchObject({ message: 'Credenciais inválidas.', fieldErrors: {}, status: 401 })
  })

  it('envia logout com nova confirmação CSRF', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'novo-csrf' })).mockResolvedValueOnce(response(null, 204))
    await logout()
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/auth/logout', expect.objectContaining({ method: 'POST', credentials: 'include' }))
  })

  it('retorna mensagem segura em falha de rede', async () => {
    fetch.mockRejectedValue(new TypeError('network details'))
    await expect(login({})).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
  })
})

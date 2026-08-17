import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAccount } from './accounts.js'

function response(body, status = 200) {
  return { ok: status >= 200 && status < 300, headers: new Headers({ 'content-type': 'application/json' }), json: vi.fn().mockResolvedValue(body) }
}

describe('createAccount', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('obtém CSRF e cria a conta com cookies incluídos', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-token' })).mockResolvedValueOnce(response({ name: 'Ana Silva' }, 201))
    const payload = { name: 'Ana Silva', cpf: '52998224725', email: 'ana@example.com', password: 'Senha@123' }
    await expect(createAccount(payload)).resolves.toEqual({ name: 'Ana Silva' })
    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/accounts', expect.objectContaining({ method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-token' } }))
  })

  it('converte todos os erros de campo retornados pela API', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-token' })).mockResolvedValueOnce(response({ message: 'Dados inválidos.', fieldErrors: [{ field: 'password', message: 'Use uma maiúscula.' }, { field: 'password', message: 'Use um número.' }, { field: 'email', message: 'E-mail inválido.' }] }, 400))
    await expect(createAccount({})).rejects.toMatchObject({ message: 'Dados inválidos.', fieldErrors: { password: ['Use uma maiúscula.', 'Use um número.'], email: ['E-mail inválido.'] } })
  })

  it('apresenta erro seguro quando não conecta ao backend', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))
    await expect(createAccount({})).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
  })

  it('interrompe o cadastro quando a resposta CSRF não contém token', async () => {
    fetch.mockResolvedValueOnce(response({}, 200))
    await expect(createAccount({})).rejects.toMatchObject({ message: 'Não foi possível iniciar o cadastro. Tente novamente.' })
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('não expõe conteúdo não JSON retornado pelo servidor', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-token' })).mockResolvedValueOnce({
      ok: false,
      headers: new Headers({ 'content-type': 'text/html' }),
      json: vi.fn(),
    })
    await expect(createAccount({})).rejects.toMatchObject({ message: 'Não foi possível criar a conta. Tente novamente.', fieldErrors: {} })
  })
})

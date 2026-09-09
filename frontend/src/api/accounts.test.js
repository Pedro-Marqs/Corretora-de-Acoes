import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountApiError, changeEmail, changePassword, checkReactivation, createAccount, deleteAccount, reactivateAccount } from './accounts.js'

function response(body, status = 200) {
  return { ok: status >= 200 && status < 300, status, headers: new Headers({ 'content-type': 'application/json' }), json: vi.fn().mockResolvedValue(body) }
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

  it('altera e-mail e senha por PATCH com CSRF', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 204))
      .mockResolvedValueOnce(response({ token: 'csrf-2' })).mockResolvedValueOnce(response(null, 204))
    await changeEmail({ newEmail: 'novo@example.com', currentPassword: 'Atual@123' })
    await changePassword({ currentPassword: 'Atual@123', newPassword: 'Nova@123' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/accounts/me/email', expect.objectContaining({ method: 'PATCH', credentials: 'include' }))
    expect(fetch).toHaveBeenNthCalledWith(4, 'http://localhost:8080/api/accounts/me/password', expect.objectContaining({ method: 'PATCH', credentials: 'include' }))
  })
})

describe('deleteAccount', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('envia o payload integral por DELETE autenticado com CSRF', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-delete' })).mockResolvedValueOnce(response(null, 204))
    const payload = { email: 'ana@example.com', password: 'Senha@123', confirmation: 'Excluir' }

    await expect(deleteAccount(payload)).resolves.toBeUndefined()

    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/accounts/me', {
      method: 'DELETE',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-delete' },
      body: JSON.stringify(payload),
    })
  })

  it('converte erro funcional com todos os erros de campo e status', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-delete' })).mockResolvedValueOnce(response({
      message: 'Confirmação inválida.',
      fieldErrors: [
        { field: 'confirmation', message: 'Confirmação deve ser exatamente Excluir.' },
        { field: 'email', message: 'E-mail inválido.' },
      ],
    }, 400))

    await expect(deleteAccount({})).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'Confirmação inválida.',
      fieldErrors: {
        confirmation: ['Confirmação deve ser exatamente Excluir.'],
        email: ['E-mail inválido.'],
      },
      status: 400,
    })
  })

  it('usa mensagem segura para resposta técnica inválida', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-delete' })).mockResolvedValueOnce({
      ok: false,
      status: 500,
      headers: new Headers({ 'content-type': 'text/html' }),
      json: vi.fn(),
    })

    await expect(deleteAccount({})).rejects.toMatchObject({
      message: 'Não foi possível excluir a conta. Tente novamente.',
      fieldErrors: {},
      status: 500,
    })
  })

  it('converte falha de conexão em AccountApiError seguro', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    const rejection = deleteAccount({})
    await expect(rejection).rejects.toBeInstanceOf(AccountApiError)
    await expect(rejection).rejects.toMatchObject({
      message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.',
    })
  })
})

describe('checkReactivation', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('consulta a disponibilidade de reativação com CSRF e retorna a resposta', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivation' })).mockResolvedValueOnce(response({ reactivationAvailable: true }))
    const result = await checkReactivation('529.982.247-25')

    expect(result).toEqual({ reactivationAvailable: true })
    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/accounts/reactivation/check', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-reactivation' },
      body: JSON.stringify({ cpf: '529.982.247-25' }),
    })
  })

  it('converte erro funcional em AccountApiError com campos agrupados e status', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivation' })).mockResolvedValueOnce(response({
      message: 'CPF inválido.',
      fieldErrors: [{ field: 'cpf', message: 'CPF deve ser válido.' }, { field: 'cpf', message: 'Informe o CPF.' }],
    }, 400))

    await expect(checkReactivation('invalido')).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'CPF inválido.',
      fieldErrors: { cpf: ['CPF deve ser válido.', 'Informe o CPF.'] },
      status: 400,
    })
  })

  it('usa mensagem segura para resposta técnica inválida', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivation' })).mockResolvedValueOnce({
      ok: false,
      status: 503,
      headers: new Headers({ 'content-type': 'text/html' }),
      json: vi.fn(),
    })

    await expect(checkReactivation('52998224725')).rejects.toMatchObject({
      message: 'Não foi possível consultar a reativação. Tente novamente.',
      fieldErrors: {},
      status: 503,
    })
  })

  it('rejeita resposta de sucesso fora do contrato da consulta', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivation' })).mockResolvedValueOnce(response({ available: true }))

    await expect(checkReactivation('52998224725')).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'A resposta da consulta de reativação não pôde ser processada.',
      fieldErrors: {},
    })
  })

  it('converte falha de conexão em AccountApiError seguro', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(checkReactivation('52998224725')).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.',
    })
  })
})

describe('reactivateAccount', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('reativa a conta com CSRF e cookies sem criar sessão no cliente', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivate' })).mockResolvedValueOnce(response(null, 204))

    await expect(reactivateAccount('529.982.247-25')).resolves.toBeUndefined()

    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/accounts/reactivation', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-reactivate' },
      body: JSON.stringify({ cpf: '529.982.247-25' }),
    })
  })

  it('converte erro funcional de reativação com campos e status', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivate' })).mockResolvedValueOnce(response({
      message: 'A conta não pode ser reativada com os dados informados.',
      fieldErrors: [{ field: 'cpf', message: 'CPF inválido.' }],
    }, 409))

    await expect(reactivateAccount('invalido')).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'A conta não pode ser reativada com os dados informados.',
      fieldErrors: { cpf: ['CPF inválido.'] },
      status: 409,
    })
  })

  it('não expõe resposta técnica não JSON', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-reactivate' })).mockResolvedValueOnce({
      ok: false,
      status: 500,
      headers: new Headers({ 'content-type': 'text/html' }),
      json: vi.fn(),
    })

    await expect(reactivateAccount('52998224725')).rejects.toMatchObject({
      message: 'Não foi possível reativar a conta. Tente novamente.',
      fieldErrors: {},
      status: 500,
    })
  })

  it('converte falha de conexão em mensagem segura', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(reactivateAccount('52998224725')).rejects.toMatchObject({
      name: 'AccountApiError',
      message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.',
    })
  })
})

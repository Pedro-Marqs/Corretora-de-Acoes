import { beforeEach, describe, expect, it, vi } from 'vitest'
import { deposit, getWalletBalance, WalletApiError } from './wallet.js'

function response(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: vi.fn().mockResolvedValue(body),
  }
}

describe('wallet API', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.stubGlobal('fetch', vi.fn())
  })

  it('consulta o saldo autenticado no endpoint da carteira', async () => {
    fetch.mockResolvedValueOnce(response({ balance: 10000 }))

    await expect(getWalletBalance()).resolves.toEqual({ balance: 10000 })
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/wallet', { credentials: 'include' })
  })

  it('envia o aporte informado com cookies e CSRF', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-wallet' }))
      .mockResolvedValueOnce(response({ balance: 10500.01 }))

    await expect(deposit('500.005')).resolves.toEqual({ balance: 10500.01 })
    expect(fetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/wallet/deposits', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-wallet' },
      body: JSON.stringify({ amount: '500.005' }),
    })
  })

  it('converte erros funcionais com campos e status', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-wallet' }))
      .mockResolvedValueOnce(response({
        message: 'Dados inválidos.',
        fieldErrors: [{ field: 'amount', message: 'O aporte mínimo é R$ 10,00.' }],
      }, 400))

    await expect(deposit('9')).rejects.toMatchObject({
      name: 'WalletApiError',
      message: 'Dados inválidos.',
      fieldErrors: { amount: ['O aporte mínimo é R$ 10,00.'] },
      status: 400,
    })
  })

  it('preserva o status de sessão inválida e usa mensagens seguras', async () => {
    fetch.mockResolvedValueOnce(response({ message: 'Autenticação necessária.' }, 401))
    await expect(getWalletBalance()).rejects.toMatchObject({ status: 401, message: 'Autenticação necessária.' })

    fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    const failure = getWalletBalance()
    await expect(failure).rejects.toBeInstanceOf(WalletApiError)
    await expect(failure).rejects.toMatchObject({
      message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.',
    })
  })

  it('rejeita respostas de sucesso sem saldo utilizável', async () => {
    fetch.mockResolvedValueOnce(response({ value: 10000 }))
    await expect(getWalletBalance()).rejects.toMatchObject({ message: 'A resposta do saldo não pôde ser processada.' })
  })

  it.each([null, false, true, '', '   '])('rejeita saldo malformado: %s', async (balance) => {
    fetch.mockResolvedValueOnce(response({ balance }))
    await expect(getWalletBalance()).rejects.toMatchObject({ message: 'A resposta do saldo não pôde ser processada.' })
  })

  it('preserva o 401 recebido ao obter CSRF para o aporte', async () => {
    fetch.mockResolvedValueOnce(response({ message: 'Autenticação necessária.' }, 401))
    await expect(deposit('100')).rejects.toMatchObject({
      name: 'WalletApiError',
      message: 'Não foi possível iniciar o aporte. Tente novamente.',
      status: 401,
    })
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})

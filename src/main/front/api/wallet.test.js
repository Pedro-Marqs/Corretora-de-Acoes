import { beforeEach, describe, expect, it, vi } from 'vitest'
import { deposit, getWalletBalance, getWalletPositions, purchaseAsset, sellAsset, WalletApiError } from './wallet.js'

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

  it('consulta e valida o snapshot autenticado de posições', async () => {
    const snapshot = { availableBalance: '9000.00', positions: [{ assetId: 'asset', brokerageId: 'broker', ticker: 'PETR4', market: 'BR', quantity: 10, averagePriceBrl: '20.00', quotePriceBrl: '25.00' }] }
    fetch.mockResolvedValueOnce(response(snapshot))
    await expect(getWalletPositions()).resolves.toEqual(snapshot)
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/wallet/positions', { credentials: 'include' })
  })

  it('rejeita snapshot sem identificadores ou valores obrigatórios', async () => {
    fetch.mockResolvedValueOnce(response({ availableBalance: '100.00', positions: [{ ticker: 'PETR4' }] }))
    await expect(getWalletPositions()).rejects.toMatchObject({ message: 'A resposta da carteira não pôde ser processada.' })
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

  it.each([
    ['compra', purchaseAsset, 'purchases', { ticker: 'PETR4', purchasedQuantity: 2, positionQuantity: 5, remainingBalanceBrl: '9900.00' }],
    ['venda', sellAsset, 'sales', { ticker: 'PETR4', soldQuantity: 2, positionQuantity: 3, remainingBalanceBrl: '10100.00' }],
  ])('envia somente a intenção mínima na %s', async (_, action, path, body) => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-operation' })).mockResolvedValueOnce(response(body))
    await expect(action('11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', 2)).resolves.toEqual(body)
    const request = fetch.mock.calls[1][1]
    expect(fetch.mock.calls[1][0]).toBe(`http://localhost:8080/api/wallet/${path}`)
    expect(JSON.parse(request.body)).toEqual({ assetId: '11111111-1111-4111-8111-111111111111', brokerId: '22222222-2222-4222-8222-222222222222', quantity: 2 })
    expect(request.body).not.toMatch(/price|exchange|balance|position|result/i)
  })

  it('preserva erro funcional e contexto retornado pelo backend', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf-operation' })).mockResolvedValueOnce(response({ message: 'Saldo insuficiente. Valor solicitado: R$ 200,00; saldo disponível: R$ 100,00.' }, 422))
    await expect(purchaseAsset('asset', 'broker', 2)).rejects.toMatchObject({ status: 422, message: expect.stringContaining('solicitado') })
  })
})

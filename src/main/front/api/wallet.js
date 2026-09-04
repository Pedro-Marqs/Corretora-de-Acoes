import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class WalletApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'WalletApiError'
  }
}

function requireBalance(body, message) {
  const balance = body?.balance
  const isNumericType = typeof balance === 'number'
    || (typeof balance === 'string' && balance.trim().length > 0)
  if (!body || typeof body !== 'object' || Array.isArray(body)
    || !isNumericType || !Number.isFinite(Number(balance))) {
    throw new WalletApiError(message)
  }
  return body
}

function validNumber(value) {
  return (typeof value === 'number' || (typeof value === 'string' && value.trim()))
    && Number.isFinite(Number(value))
}

function requirePositions(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)
    || !validNumber(body.availableBalance) || !Array.isArray(body.positions)) {
    throw new WalletApiError('A resposta da carteira não pôde ser processada.')
  }
  const valid = body.positions.every((position) => position
    && typeof position.assetId === 'string' && position.assetId
    && typeof position.brokerageId === 'string' && position.brokerageId
    && typeof position.ticker === 'string' && position.ticker
    && ['BR', 'US'].includes(position.market)
    && validNumber(position.quantity) && Number(position.quantity) > 0
    && validNumber(position.averagePriceBrl))
  if (!valid) throw new WalletApiError('A resposta da carteira não pôde ser processada.')
  return body
}

export async function getWalletPositions() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/wallet/positions`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) throw new WalletApiError(body?.message ?? 'Não foi possível consultar a carteira.', groupFieldErrors(body?.fieldErrors), response.status)
    return requirePositions(body)
  } catch (error) {
    if (error instanceof WalletApiError) throw error
    throw new WalletApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

function requireOperation(body, operation) {
  const quantityField = operation === 'purchase' ? 'purchasedQuantity' : 'soldQuantity'
  if (!body || typeof body !== 'object' || Array.isArray(body)
    || !validNumber(body.remainingBalanceBrl) || !validNumber(body.positionQuantity)
    || !validNumber(body[quantityField]) || typeof body.ticker !== 'string') {
    throw new WalletApiError('A resposta da operação não pôde ser processada.')
  }
  return body
}

async function operate(path, operation, assetId, brokerId, quantity) {
  try {
    const token = await getCsrfToken(WalletApiError, 'Não foi possível iniciar a operação. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/wallet/${path}`, {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify({ assetId, brokerId, quantity }),
    })
    const body = await parseJson(response)
    if (!response.ok) throw new WalletApiError(body?.message ?? 'Não foi possível concluir a operação.', groupFieldErrors(body?.fieldErrors), response.status)
    return requireOperation(body, operation)
  } catch (error) {
    if (error instanceof WalletApiError) throw error
    throw new WalletApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export function purchaseAsset(assetId, brokerId, quantity) {
  return operate('purchases', 'purchase', assetId, brokerId, quantity)
}

export function sellAsset(assetId, brokerId, quantity) {
  return operate('sales', 'sale', assetId, brokerId, quantity)
}

export async function getWalletBalance() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/wallet`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) {
      throw new WalletApiError(
        body?.message ?? 'Não foi possível consultar o saldo.',
        groupFieldErrors(body?.fieldErrors),
        response.status,
      )
    }
    return requireBalance(body, 'A resposta do saldo não pôde ser processada.')
  } catch (error) {
    if (error instanceof WalletApiError) throw error
    throw new WalletApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function deposit(amount) {
  try {
    const token = await getCsrfToken(WalletApiError, 'Não foi possível iniciar o aporte. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/wallet/deposits`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify({ amount }),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      throw new WalletApiError(
        body?.message ?? 'Não foi possível realizar o aporte. Tente novamente.',
        groupFieldErrors(body?.fieldErrors),
        response.status,
      )
    }
    return requireBalance(body, 'A resposta do aporte não pôde ser processada.')
  } catch (error) {
    if (error instanceof WalletApiError) throw error
    throw new WalletApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

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

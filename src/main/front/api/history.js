import { API_BASE_URL, ApiError, groupFieldErrors, parseJson } from './http.js'

export class HistoryApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'HistoryApiError'
  }
}

const TYPES = ['INITIAL_BALANCE', 'DEPOSIT', 'PURCHASE', 'SALE', 'TRANSFER']

function numeric(value) {
  return (typeof value === 'number' || (typeof value === 'string' && value.trim())) && Number.isFinite(Number(value))
}

function validMovement(item) {
  return item && typeof item === 'object' && typeof item.id === 'string'
    && TYPES.includes(item.type) && typeof item.occurredAt === 'string'
    && numeric(item.remainingBalance)
}

function requireHistoryPage(body) {
  const valid = body && typeof body === 'object' && !Array.isArray(body)
    && Array.isArray(body.content) && body.content.length <= 20
    && Number.isInteger(body.page) && body.page >= 0
    && body.size === 20 && Number.isInteger(body.totalPages) && body.totalPages >= 0
    && Number.isInteger(body.totalElements) && body.totalElements >= 0
    && body.content.every(validMovement)
  if (!valid) throw new HistoryApiError('A resposta do histórico não pôde ser processada.')
  return body
}

export async function getHistory({ page = 0, from, to, type, ticker, brokerId, market } = {}) {
  const params = new URLSearchParams({ page: String(page) })
  for (const [name, value] of Object.entries({ from, to, type, ticker, brokerId, market })) {
    if (typeof value === 'string' && value.trim()) params.set(name, value.trim())
  }
  try {
    const response = await fetch(`${API_BASE_URL}/api/history?${params}`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) throw new HistoryApiError(body?.message ?? 'Não foi possível consultar o histórico.', groupFieldErrors(body?.fieldErrors), response.status)
    return requireHistoryPage(body)
  } catch (error) {
    if (error instanceof HistoryApiError) throw error
    throw new HistoryApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

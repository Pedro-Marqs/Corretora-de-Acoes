import { ApiError, API_BASE_URL, groupFieldErrors } from './http.js'
import { adminFetch, parseAdminResponse } from './admin-http.js'

export class AdminDashboardApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'AdminDashboardApiError'
  }
}

const CONVERSION_STATUSES = ['AVAILABLE', 'UNAVAILABLE', 'NOT_REQUIRED']

function number(value) { return (typeof value === 'number' || typeof value === 'string') && Number.isFinite(Number(value)) }
function date(value) { return typeof value === 'string' && !Number.isNaN(new Date(value).getTime()) }
function volume(value) {
  return value && number(value.brl) && number(value.usd) && (value.equivalentBrl == null || number(value.equivalentBrl))
    && CONVERSION_STATUSES.includes(value.conversionStatus) && (value.usdBrlRate == null || number(value.usdBrlRate))
    && (value.rateQuotedAt == null || date(value.rateQuotedAt))
}
function validBody(body) {
  return body && typeof body === 'object' && !Array.isArray(body)
    && ['totalAccounts', 'activeAccounts', 'inactiveAccounts', 'newAccounts', 'purchaseCount', 'saleCount', 'purchasedShares', 'soldShares'].every((field) => Number.isInteger(Number(body[field])) && Number(body[field]) >= 0)
    && volume(body.purchaseVolume) && volume(body.saleVolume)
    && Array.isArray(body.series) && body.series.every((item) => item && date(item.date) && Number.isInteger(Number(item.newAccounts)) && Number.isInteger(Number(item.purchaseCount)) && Number.isInteger(Number(item.saleCount)))
    && Array.isArray(body.ranking) && body.ranking.every((item) => item && typeof item.ticker === 'string' && item.ticker && ['BR', 'US'].includes(item.market) && Number.isInteger(Number(item.tradedShares)))
}

export async function getAdminDashboard({ period = '4W', limit = 10 } = {}) {
  const params = new URLSearchParams({ period, limit: String(limit) })
  try {
    const response = await adminFetch(`${API_BASE_URL}/api/admin/dashboard?${params}`)
    const body = await parseAdminResponse(response)
    if (!response.ok) throw new AdminDashboardApiError(body?.message ?? 'Não foi possível carregar o dashboard administrativo.', groupFieldErrors(body?.fieldErrors), response.status)
    if (!validBody(body)) throw new AdminDashboardApiError('A resposta do dashboard administrativo não pôde ser processada.')
    return body
  } catch (error) {
    if (error instanceof AdminDashboardApiError) throw error
    throw new AdminDashboardApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

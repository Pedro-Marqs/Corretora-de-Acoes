import { API_BASE_URL, ApiError, groupFieldErrors, parseJson } from './http.js'

export class DashboardApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'DashboardApiError'
  }
}

const PERIODS = ['4W', '3M', '6M', '1Y', '5Y', 'MAX']
const MARKETS = ['BR', 'US']
const CURRENCIES = ['BRL', 'USD']
const WARNING_TYPES = ['STALE_QUOTE', 'STALE_EXCHANGE_RATE']

function numeric(value) {
  return (typeof value === 'number' || (typeof value === 'string' && value.trim())) && Number.isFinite(Number(value))
}

function nullableString(value) { return value == null || typeof value === 'string' }
function validDate(value) { return typeof value === 'string' && !Number.isNaN(new Date(value).getTime()) }

function validPosition(item) {
  return item && typeof item === 'object'
    && ['ticker', 'name', 'brokerageName'].every((field) => typeof item[field] === 'string' && item[field].trim())
    && MARKETS.includes(item.market) && CURRENCIES.includes(item.currency)
    && Number.isInteger(item.quantity) && item.quantity > 0
    && ['averagePriceBrl', 'totalCostBrl', 'quotePrice', 'quotePriceBrl', 'marketValueBrl', 'unrealizedResultBrl'].every((field) => numeric(item[field]))
    && validDate(item.quoteQuotedAt) && typeof item.quoteStale === 'boolean'
}

function validSlice(item) {
  return item && typeof item.identifier === 'string' && item.identifier.trim()
    && typeof item.label === 'string' && item.label.trim() && numeric(item.valueBrl)
}

function validDistributions(value) {
  return value && ['byAsset', 'byBroker', 'byMarket'].every((field) => Array.isArray(value[field]) && value[field].every(validSlice))
}

function validExchangeRate(value) {
  return value === null || (value && typeof value.currencyPair === 'string' && numeric(value.rate)
    && validDate(value.quotedAt) && typeof value.stale === 'boolean')
}

function validWarning(value) {
  return value && WARNING_TYPES.includes(value.type) && nullableString(value.ticker) && validDate(value.observedAt)
}

function validPoint(value) { return value && validDate(value.recordedAt) && numeric(value.patrimonyBrl) }

function requireDashboard(body) {
  const valid = body && typeof body === 'object' && !Array.isArray(body)
    && ['availableBalanceBrl', 'positionsMarketValueBrl', 'patrimonyBrl', 'realizedResultBrl', 'unrealizedResultBrl', 'totalResultBrl'].every((field) => numeric(body[field]))
    && body.balanceShared === true && nullableString(body.selectedBrokerAssociationId)
    && Array.isArray(body.positions) && body.positions.every(validPosition)
    && validDistributions(body.distributions) && validExchangeRate(body.exchangeRate ?? null)
    && Array.isArray(body.warnings) && body.warnings.every(validWarning)
    && PERIODS.includes(body.period) && Array.isArray(body.patrimonyHistory) && body.patrimonyHistory.every(validPoint)
  if (!valid) throw new DashboardApiError('A resposta do dashboard não pôde ser processada.')
  return body
}

export async function getDashboard({ brokerAssociationId, period = '4W' } = {}) {
  const params = new URLSearchParams({ period })
  if (typeof brokerAssociationId === 'string' && brokerAssociationId.trim()) params.set('brokerAssociationId', brokerAssociationId.trim())
  try {
    const response = await fetch(`${API_BASE_URL}/api/dashboard?${params}`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) throw new DashboardApiError(body?.message ?? 'Não foi possível consultar o dashboard.', groupFieldErrors(body?.fieldErrors), response.status)
    return requireDashboard(body)
  } catch (error) {
    if (error instanceof DashboardApiError) throw error
    throw new DashboardApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

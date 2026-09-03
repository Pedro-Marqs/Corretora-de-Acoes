import { API_BASE_URL, ApiError, groupFieldErrors, parseJson } from './http.js'

export class MarketApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) { super(message, fieldErrors, status); this.name = 'MarketApiError' }
}

function validText(value) { return typeof value === 'string' && value.trim().length > 0 }
function validNumber(value) { return (typeof value === 'number' || validText(value)) && Number.isFinite(Number(value)) }
function validInstant(value) { return validText(value) && !Number.isNaN(new Date(value).getTime()) }

function requireAsset(body) {
  if (body === null) return null
  const common = body && typeof body === 'object' && !Array.isArray(body)
    && validText(body.ticker) && validText(body.name) && ['BR', 'US'].includes(body.market)
    && ['BRL', 'USD'].includes(body.currency) && validNumber(body.originalPrice)
    && validNumber(body.priceBrl) && validText(body.quoteSource) && validInstant(body.quoteQuotedAt)
    && typeof body.quoteStale === 'boolean'
    && ((body.market === 'BR' && body.currency === 'BRL') || (body.market === 'US' && body.currency === 'USD'))
  const us = body?.market !== 'US' || (body.currency === 'USD' && validNumber(body.usdBrlRate)
    && validText(body.exchangeRateSource) && validInstant(body.exchangeRateQuotedAt)
    && typeof body.exchangeRateStale === 'boolean')
  if (!common || !us) throw new MarketApiError('A resposta do ativo está incompleta. Tente novamente mais tarde.')
  return body
}

export async function searchAsset(rawTicker, rawMarket) {
  const ticker = String(rawTicker ?? '').trim().toUpperCase()
  const market = String(rawMarket ?? '').trim().toUpperCase()
  if (!['BR', 'US'].includes(market)) {
    throw new MarketApiError('Selecione um mercado válido.')
  }
  try {
    const query = new URLSearchParams({ ticker, market })
    const response = await fetch(`${API_BASE_URL}/api/assets/search?${query}`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) throw new MarketApiError(body?.message ?? 'Não foi possível pesquisar o ativo.', groupFieldErrors(body?.fieldErrors), response.status)
    return requireAsset(body)
  } catch (error) {
    if (error instanceof MarketApiError) throw error
    throw new MarketApiError('Não foi possível conectar ao servidor. Tente novamente em instantes.')
  }
}

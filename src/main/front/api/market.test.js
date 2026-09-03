import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MarketApiError, searchAsset } from './market.js'
function response(body, status = 200) { return { ok: status >= 200 && status < 300, status, headers: new Headers({ 'content-type': 'application/json' }), json: vi.fn().mockResolvedValue(body) } }
const br = { ticker: 'PETR4', name: 'Petrobras', market: 'BR', currency: 'BRL', originalPrice: 38.5, priceBrl: 38.5, quoteSource: 'Brapi', quoteQuotedAt: '2026-09-03T12:00:00Z', quoteStale: false, usdBrlRate: null, exchangeRateSource: null, exchangeRateQuotedAt: null, exchangeRateStale: null }
const us = { ticker: 'AAPL', name: 'Apple Inc.', market: 'US', currency: 'USD', originalPrice: '225.10', priceBrl: '1238.05', quoteSource: 'Twelve Data', quoteQuotedAt: '2026-09-02T20:00:00Z', quoteStale: false, usdBrlRate: '5.50', exchangeRateSource: 'AwesomeAPI', exchangeRateQuotedAt: '2026-09-03T10:00:00Z', exchangeRateStale: false }
describe('market API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })
  it.each([[' petr4 ', 'PETR4', 'BR', br], ['aapl', 'AAPL', 'US', us]])('pesquisa %s usando ticker e sessão', async (input, ticker, market, body) => {
    fetch.mockResolvedValue(response(body)); await expect(searchAsset(input)).resolves.toEqual(body)
    expect(fetch).toHaveBeenCalledWith(`http://localhost:8080/api/assets/search?ticker=${ticker}&market=${market}`, { credentials: 'include' })
    expect(fetch.mock.calls[0][1]).not.toHaveProperty('body'); expect(fetch.mock.calls[0][1]).not.toHaveProperty('method')
  })
  it('preserva erro funcional e status', async () => { fetch.mockResolvedValue(response({ message: 'Mercado não suportado.' }, 422)); await expect(searchAsset('XPTO')).rejects.toMatchObject({ message: 'Mercado não suportado.', status: 422 }) })
  it('normaliza falha de transporte', async () => { fetch.mockRejectedValue(new TypeError('internal detail')); await expect(searchAsset('AAPL')).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Tente novamente em instantes.' }) })
  it.each([{}, { ...br, name: null }, { ...br, originalPrice: null }, { ...br, quoteSource: null }, { ...br, quoteQuotedAt: 'invalid' }, { ...br, currency: 'USD' }, { ...us, usdBrlRate: null }, { ...us, exchangeRateSource: null }, { ...us, exchangeRateQuotedAt: null }])('rejeita resposta incompleta: %#', async (body) => { fetch.mockResolvedValue(response(body)); await expect(searchAsset('AAPL')).rejects.toBeInstanceOf(MarketApiError) })
  it('representa ausência como vazio', async () => { fetch.mockResolvedValue(response(null, 204)); await expect(searchAsset('AAPL')).resolves.toBeNull() })
})

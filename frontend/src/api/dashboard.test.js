import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DashboardApiError, getDashboard } from './dashboard.js'

const position = { ticker: 'PETR4', name: 'Petrobras', market: 'BR', currency: 'BRL', brokerageName: 'Corretora Um', quantity: 10, averagePriceBrl: 20, totalCostBrl: 200, quotePrice: 25, quotePriceBrl: 25, marketValueBrl: 250, unrealizedResultBrl: 50, quoteQuotedAt: '2026-09-08T10:00:00-03:00', quoteStale: false }
const slice = { identifier: 'PETR4', label: 'PETR4', valueBrl: 250 }
const dashboard = { availableBalanceBrl: 1000, balanceShared: true, selectedBrokerAssociationId: null, positions: [position], positionsMarketValueBrl: 250, patrimonyBrl: 1250, realizedResultBrl: 10, unrealizedResultBrl: 50, totalResultBrl: 60, distributions: { byAsset: [slice], byBroker: [slice], byMarket: [slice] }, exchangeRate: null, warnings: [], period: '4W', patrimonyHistory: [{ recordedAt: '2026-09-01T10:00:00-03:00', patrimonyBrl: 1000 }] }
function response(body, status = 200, contentType = 'application/json') { return { ok: status >= 200 && status < 300, status, headers: new Headers(contentType ? { 'content-type': contentType } : {}), json: vi.fn().mockResolvedValue(body) } }

describe('dashboard API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('envia período e visão geral com cookie de sessão', async () => {
    fetch.mockResolvedValue(response(dashboard))
    await expect(getDashboard({ period: '3M' })).resolves.toBe(dashboard)
    expect(fetch).toHaveBeenCalledWith('/api/dashboard?period=3M', { credentials: 'include' })
  })

  it('envia a associação selecionada sem parâmetros extras', async () => {
    fetch.mockResolvedValue(response({ ...dashboard, selectedBrokerAssociationId: 'broker-1' }))
    await getDashboard({ period: 'MAX', brokerAssociationId: ' broker-1 ' })
    expect(fetch).toHaveBeenCalledWith('/api/dashboard?period=MAX&brokerAssociationId=broker-1', { credentials: 'include' })
  })

  it('preserva status, mensagem e erros funcionais', async () => {
    fetch.mockResolvedValue(response({ message: 'Período inválido.', fieldErrors: [{ field: 'period', message: 'Revise o período.' }] }, 400))
    await expect(getDashboard()).rejects.toMatchObject({ name: 'DashboardApiError', status: 400, message: 'Período inválido.', fieldErrors: { period: ['Revise o período.'] } })
  })

  it('normaliza falha de transporte sem expor detalhes técnicos', async () => {
    fetch.mockRejectedValue(new TypeError('socket secret'))
    await expect(getDashboard()).rejects.toMatchObject({ message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
  })

  it.each([null, [], {}, { ...dashboard, balanceShared: false }, { ...dashboard, period: '1M' }, { ...dashboard, positions: [{ ...position, marketValueBrl: 'inválido' }] }, { ...dashboard, distributions: { byAsset: [], byBroker: [], byMarket: [{}] } }, { ...dashboard, patrimonyHistory: [{ recordedAt: 'inválida', patrimonyBrl: 1 }] }])('rejeita resposta incompatível %#', async (body) => {
    fetch.mockResolvedValue(response(body))
    await expect(getDashboard()).rejects.toBeInstanceOf(DashboardApiError)
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAdminDashboard } from './admin-dashboard.js'
import { beginAdminSession, endAdminSession, setAdminAccessTokenForGeneration } from './admin-auth-token.js'

function response(body, status = 200) {
  return { ok: status >= 200 && status < 300, status, headers: new Headers(body ? { 'content-type': 'application/json' } : {}), json: vi.fn().mockResolvedValue(body) }
}
const valid = { totalAccounts: 2, activeAccounts: 1, inactiveAccounts: 1, newAccounts: 2, purchaseCount: 3, saleCount: 1, purchasedShares: 20, soldShares: 5, purchaseVolume: { brl: 100, usd: 10, equivalentBrl: 150, conversionStatus: 'AVAILABLE', usdBrlRate: 5, rateQuotedAt: '2026-01-01T12:00:00Z' }, saleVolume: { brl: 20, usd: 0, equivalentBrl: 20, conversionStatus: 'UNAVAILABLE', usdBrlRate: null, rateQuotedAt: null }, series: [{ date: '2026-01-01', newAccounts: 2, purchaseCount: 3, saleCount: 1 }], ranking: [{ ticker: 'PETR4', market: 'BR', tradedShares: 20 }] }

describe('admin dashboard API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()); endAdminSession(); const generation = beginAdminSession(); setAdminAccessTokenForGeneration('admin', generation) })

  it('consulta o contrato agregado com período e limite', async () => {
    fetch.mockResolvedValue(response(valid))
    await expect(getAdminDashboard({ period: '3M', limit: 10 })).resolves.toEqual(valid)
    expect(fetch).toHaveBeenCalledWith('/api/admin/dashboard?period=3M&limit=10', expect.objectContaining({ credentials: 'omit' }))
    expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer admin')
  })

  it('rejeita resposta estruturalmente incompleta', async () => {
    fetch.mockResolvedValue(response({ ...valid, purchaseVolume: null }))
    await expect(getAdminDashboard()).rejects.toMatchObject({ name: 'AdminDashboardApiError' })
  })

  it('rejeita campos numéricos, datas e conversões inválidos', async () => {
    fetch.mockResolvedValue(response({ ...valid, purchaseVolume: { ...valid.purchaseVolume, brl: 'not-a-number' }, series: [{ date: 'invalid', newAccounts: 0, purchaseCount: 0, saleCount: 0 }] }))
    await expect(getAdminDashboard()).rejects.toMatchObject({ name: 'AdminDashboardApiError' })
    fetch.mockResolvedValue(response({ ...valid, saleVolume: { ...valid.saleVolume, conversionStatus: 'BROKEN' }, ranking: [{ ticker: '', market: 'BR', tradedShares: 1 }] }))
    await expect(getAdminDashboard()).rejects.toMatchObject({ name: 'AdminDashboardApiError' })
  })

  it('accepts conversion as not required when there is no USD volume', async () => {
    const noConversion = { ...valid, purchaseVolume: { brl: 100, usd: 0, equivalentBrl: null, conversionStatus: 'NOT_REQUIRED', usdBrlRate: null, rateQuotedAt: null } }
    fetch.mockResolvedValue(response(noConversion))
    await expect(getAdminDashboard()).resolves.toEqual(noConversion)
  })
})

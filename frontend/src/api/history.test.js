import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getHistory, HistoryApiError } from './history.js'

const movement = { id: 'movement-1', type: 'PURCHASE', occurredAt: '2026-09-05T14:30:00-03:00', remainingBalance: 9500 }
const page = { content: [movement], page: 2, size: 20, totalPages: 4, totalElements: 61 }

function response(body, status = 200, contentType = 'application/json') {
  return { ok: status >= 200 && status < 300, status, headers: new Headers(contentType ? { 'content-type': contentType } : {}), json: vi.fn().mockResolvedValue(body) }
}

describe('history API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()) })

  it('serializa página zero-based e filtros combinados sem enviar size', async () => {
    fetch.mockResolvedValue(response(page))
    await expect(getHistory({ page: 2, from: ' 2026-09-01T00:00:00-03:00 ', to: '2026-09-05T23:59:00-03:00', type: ' PURCHASE ', ticker: ' PETR4 ', brokerId: ' broker-1 ', market: ' BR ', size: 99 })).resolves.toEqual(page)
    expect(fetch).toHaveBeenCalledWith('/api/history?page=2&from=2026-09-01T00%3A00%3A00-03%3A00&to=2026-09-05T23%3A59%3A00-03%3A00&type=PURCHASE&ticker=PETR4&brokerId=broker-1&market=BR', { credentials: 'include' })
  })

  it('omite filtros vazios e usa a primeira página por padrão', async () => {
    fetch.mockResolvedValue(response({ ...page, page: 0 }))
    await getHistory({ ticker: '   ' })
    expect(fetch).toHaveBeenCalledWith('/api/history?page=0', { credentials: 'include' })
  })

  it('preserva status, mensagem e erros funcionais agrupados', async () => {
    fetch.mockResolvedValue(response({ message: 'Filtro inválido.', fieldErrors: [{ field: 'ticker', message: 'Ticker inválido.' }, { field: 'ticker', message: 'Revise o ticker.' }] }, 400))
    await expect(getHistory()).rejects.toMatchObject({ name: 'HistoryApiError', status: 400, message: 'Filtro inválido.', fieldErrors: { ticker: ['Ticker inválido.', 'Revise o ticker.'] } })
  })

  it('normaliza falha de transporte sem expor detalhes técnicos', async () => {
    fetch.mockRejectedValue(new TypeError('socket failed with secret'))
    await expect(getHistory()).rejects.toMatchObject({ name: 'HistoryApiError', message: 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.' })
  })

  it.each([
    null,
    [],
    {},
    { ...page, size: 50 },
    { ...page, content: Array.from({ length: 21 }, (_, index) => ({ ...movement, id: `movement-${index}` })) },
    { ...page, content: [{ ...movement, type: 'UNKNOWN' }] },
    { ...page, content: [{ ...movement, remainingBalance: 'not-a-number' }] },
  ])('rejeita resposta incompatível com o contrato %#', async (body) => {
    fetch.mockResolvedValue(response(body))
    await expect(getHistory()).rejects.toBeInstanceOf(HistoryApiError)
    await expect(getHistory()).rejects.toMatchObject({ message: 'A resposta do histórico não pôde ser processada.' })
  })
})

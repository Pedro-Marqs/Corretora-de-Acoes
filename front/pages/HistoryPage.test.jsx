import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { getHistory, HistoryApiError } from '../api/history.js'
import { AuthContext } from '../context/auth-context.js'
import HistoryPage from './HistoryPage.jsx'

vi.mock('../api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('../api/history.js', async (load) => ({ ...await load(), getHistory: vi.fn() }))

const broker = { associationId: 'broker-1', tradeName: 'Corretora Um' }
const movements = [
  { id: 'initial', type: 'INITIAL_BALANCE', occurredAt: '2026-09-01T10:00:00-03:00', totalAmount: 10000, remainingBalance: 10000 },
  { id: 'deposit', type: 'DEPOSIT', occurredAt: '2026-09-02T10:00:00-03:00', totalAmount: 500, remainingBalance: 10500 },
  { id: 'purchase', type: 'PURCHASE', occurredAt: '2026-09-03T10:00:00-03:00', ticker: 'PETR4', market: 'BR', quantity: 10, brokerName: 'Corretora Um', quotePrice: 30, currency: 'BRL', unitPriceBrl: 30, totalAmount: 300, remainingBalance: 10200 },
  { id: 'sale', type: 'SALE', occurredAt: '2026-09-04T10:00:00-03:00', ticker: 'AAPL', market: 'US', quantity: 2, brokerName: 'Corretora Um', quotePrice: 200, currency: 'USD', unitPriceBrl: 1100, totalAmount: 2200, realizedResult: 200, remainingBalance: 12400 },
  { id: 'transfer', type: 'TRANSFER', occurredAt: '2026-09-05T10:00:00-03:00', ticker: 'VALE3', market: 'BR', quantity: 3, originBrokerName: 'Origem', destinationBrokerName: 'Destino', totalAmount: 180, remainingBalance: 12400 },
]
const result = { content: movements, page: 0, size: 20, totalPages: 2, totalElements: 25 }

function deferred() {
  let resolve
  let reject
  const promise = new Promise((done, fail) => { resolve = done; reject = fail })
  return { promise, resolve, reject }
}

function setup(auth = { clear: vi.fn() }) {
  render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/app/historico']}><Routes><Route path="/app/historico" element={<HistoryPage />} /><Route path="/login" element={<p>Login destino</p>} /></Routes></MemoryRouter></AuthContext.Provider>)
  return auth
}

afterEach(cleanup)

describe('HistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getActiveBrokers.mockResolvedValue([broker])
    getHistory.mockResolvedValue(result)
  })

  it('cobre carregamento, tipos condicionais e estrutura semântica somente leitura', async () => {
    const pending = deferred()
    getHistory.mockReturnValue(pending.promise)
    setup()
    expect(screen.getByRole('status')).toHaveTextContent('Carregando histórico')
    pending.resolve(result)
    expect(await screen.findByText('Saldo inicial')).toBeInTheDocument()
    const historyList = document.querySelector('.history-list')
    for (const label of ['Aporte', 'Compra', 'Venda', 'Transferência']) expect(within(historyList).getByText(label)).toBeInTheDocument()
    expect(document.querySelectorAll('dl.history-card-grid')).toHaveLength(5)
    expect(screen.getByText('Resultado realizado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /editar|excluir|estornar/i })).not.toBeInTheDocument()
  })

  it('combina filtros, normaliza ticker, reinicia a página e os preserva na paginação', async () => {
    setup(); await screen.findByText('Compra')
    fireEvent.click(screen.getByRole('button', { name: 'Próxima página' }))
    await waitFor(() => expect(getHistory).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })))
    fireEvent.change(screen.getByLabelText('Data inicial'), { target: { value: '2026-09-01T08:30' } })
    fireEvent.change(screen.getByLabelText('Data final'), { target: { value: '2026-09-05T18:00' } })
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'PURCHASE' } })
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'petr4' } })
    fireEvent.change(screen.getByLabelText('Corretora'), { target: { value: 'broker-1' } })
    fireEvent.change(screen.getByLabelText('Mercado'), { target: { value: 'BR' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await waitFor(() => expect(getHistory).toHaveBeenLastCalledWith({ page: 0, from: '2026-09-01T08:30:00-03:00', to: '2026-09-05T18:00:00-03:00', type: 'PURCHASE', ticker: 'PETR4', brokerId: 'broker-1', market: 'BR' }))
    fireEvent.click(screen.getByRole('button', { name: 'Próxima página' }))
    await waitFor(() => expect(getHistory).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, ticker: 'PETR4', brokerId: 'broker-1' })))
  })

  it('valida intervalo e ticker, associa erros e não consulta a API', async () => {
    setup(); await screen.findByText('Compra'); getHistory.mockClear()
    fireEvent.change(screen.getByLabelText('Data inicial'), { target: { value: '2026-09-06T10:00' } })
    fireEvent.change(screen.getByLabelText('Data final'), { target: { value: '2026-09-05T10:00' } })
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'PETR-4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    expect(screen.getByLabelText('Data inicial')).toHaveAttribute('aria-describedby', 'history-interval-error')
    expect(screen.getByLabelText('Ticker')).toHaveAttribute('aria-describedby', 'history-ticker-error')
    expect(getHistory).not.toHaveBeenCalled()
  })

  it('exibe vazio sem paginação', async () => {
    getHistory.mockResolvedValue({ content: [], page: 0, size: 20, totalPages: 0, totalElements: 0 })
    setup()
    expect(await screen.findByText('Nenhuma movimentação encontrada')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Paginação do histórico' })).not.toBeInTheDocument()
  })

  it('mostra erro seguro, preserva filtros e permite nova tentativa', async () => {
    getHistory.mockRejectedValueOnce(new HistoryApiError('Histórico temporariamente indisponível.')).mockResolvedValueOnce(result)
    setup()
    expect(await screen.findByRole('alert')).toHaveTextContent('Histórico temporariamente indisponível.')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'PETR4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('Compra')).toBeInTheDocument()
    expect(screen.getByLabelText('Ticker')).toHaveValue('PETR4')
  })

  it('trata falha de corretoras sem bloquear o histórico e oferece retry', async () => {
    getActiveBrokers.mockRejectedValueOnce(new Error('Corretoras temporariamente indisponíveis.')).mockResolvedValueOnce([broker])
    setup()
    expect(await screen.findByText('Compra')).toBeInTheDocument()
    const warning = screen.getByRole('alert')
    expect(warning).toHaveTextContent('Corretoras temporariamente indisponíveis.')
    fireEvent.click(within(warning).getByRole('button', { name: 'Tentar carregar corretoras novamente' }))
    await waitFor(() => expect(screen.getByLabelText('Corretora')).toHaveTextContent('Corretora Um'))
    expect(getActiveBrokers).toHaveBeenCalledTimes(2)
  })

  it.each(['history', 'brokers'])('limpa dados privados e navega ao login em 401 de %s', async (source) => {
    if (source === 'history') getHistory.mockRejectedValue(new HistoryApiError('Sessão expirada.', {}, 401))
    else getActiveBrokers.mockRejectedValue({ status: 401 })
    const auth = setup()
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(auth.clear).toHaveBeenCalled()
    expect(screen.queryByText('PETR4')).not.toBeInTheDocument()
  })

  it('descarta resposta obsoleta de filtros anteriores', async () => {
    setup(); await screen.findByText('Compra')
    const oldRequest = deferred()
    const currentRequest = deferred()
    getHistory.mockReturnValueOnce(oldRequest.promise).mockReturnValueOnce(currentRequest.promise)
    const form = document.querySelector('.history-filters')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'PETR4' } }); fireEvent.submit(form)
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } }); fireEvent.submit(form)
    currentRequest.resolve({ ...result, content: [{ ...movements[2], id: 'current', ticker: 'VALE3' }] })
    expect(await screen.findByText(/VALE3/)).toBeInTheDocument()
    oldRequest.resolve({ ...result, content: [{ ...movements[2], id: 'old', ticker: 'PETR4' }] })
    await waitFor(() => expect(screen.queryByText(/PETR4/)).not.toBeInTheDocument())
  })

  it('exibe no máximo os 20 itens oficiais e bloqueia consulta duplicada equivalente', async () => {
    const twenty = Array.from({ length: 20 }, (_, index) => ({ ...movements[0], id: `item-${index}` }))
    getHistory.mockResolvedValue({ ...result, content: twenty, totalElements: 20, totalPages: 1 })
    setup(); await waitFor(() => expect(document.querySelectorAll('.history-card')).toHaveLength(20))
    getHistory.mockClear()
    const pending = deferred(); getHistory.mockReturnValue(pending.promise)
    const form = document.querySelector('.history-filters')
    fireEvent.submit(form); fireEvent.submit(form)
    expect(getHistory).toHaveBeenCalledTimes(1)
  })

  it.each([320, 768, 1280])('mantém estrutura responsiva e controles no viewport de %d px', async (width) => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: width })
    setup(); await screen.findByText('Compra')
    expect(document.querySelector('.history-page')).toBeInTheDocument()
    expect(document.querySelector('.history-filters')).toBeInTheDocument()
    expect(document.querySelector('.history-card-grid')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Paginação do histórico' })).toBeInTheDocument()
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(document.documentElement.clientWidth)
  })
})

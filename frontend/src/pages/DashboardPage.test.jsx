import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { DashboardApiError, getDashboard } from '../api/dashboard.js'
import { AuthContext } from '../context/auth-context.js'
import DashboardPage from './DashboardPage.jsx'

vi.mock('../api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('../api/dashboard.js', async (load) => ({ ...await load(), getDashboard: vi.fn() }))

const brokers = [{ associationId: 'broker-1', tradeName: 'Corretora Um' }, { associationId: 'broker-2', tradeName: 'Corretora Dois' }]
const positions = [{ ticker: 'PETR4', name: 'Petrobras', market: 'BR', currency: 'BRL', brokerageName: 'Corretora Um', quantity: 10, averagePriceBrl: 20, totalCostBrl: 200, quotePrice: 25, quotePriceBrl: 25, marketValueBrl: 250, unrealizedResultBrl: 50, quoteQuotedAt: '2026-09-08T13:00:00Z', quoteStale: false }]
const complete = { availableBalanceBrl: '1000.10', balanceShared: true, selectedBrokerAssociationId: null, positions, positionsMarketValueBrl: '250.20', patrimonyBrl: '1250.30', realizedResultBrl: '10.40', unrealizedResultBrl: '50.50', totalResultBrl: '60.60', distributions: { byAsset: [{ identifier: 'PETR4', label: 'PETR4', valueBrl: '250.20' }], byBroker: [{ identifier: 'broker-1', label: 'Corretora Um', valueBrl: '250.20' }], byMarket: [{ identifier: 'BR', label: 'Brasil', valueBrl: '250.20' }] }, exchangeRate: null, warnings: [], period: '4W', patrimonyHistory: [{ recordedAt: '2026-08-20T10:00:00-03:00', patrimonyBrl: '1100.11' }, { recordedAt: '2026-09-01T10:00:00-03:00', patrimonyBrl: '1250.30' }] }

function deferred() { let resolve; let reject; const promise = new Promise((done, fail) => { resolve = done; reject = fail }); return { promise, resolve, reject } }
function setup(auth = { clear: vi.fn() }) { render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/app/dashboard']}><Routes><Route path="/app/dashboard" element={<DashboardPage />} /><Route path="/login" element={<p>Login destino</p>} /></Routes></MemoryRouter></AuthContext.Provider>); return auth }
afterEach(cleanup)

describe('DashboardPage', () => {
  beforeEach(() => { vi.clearAllMocks(); getActiveBrokers.mockResolvedValue(brokers); getDashboard.mockResolvedValue(complete) })

  it('mantém transferências e histórico acessíveis dentro de Investimentos', async () => {
    setup()
    expect(await screen.findByRole('heading', { name: 'Investimentos' })).toBeInTheDocument()
    const shortcuts = screen.getByRole('navigation', { name: 'Ações de investimentos' })
    expect(within(shortcuts).getByRole('link', { name: 'Transferências' })).toHaveAttribute('href', '/app/transferencias')
    expect(within(shortcuts).getByRole('link', { name: 'Histórico' })).toHaveAttribute('href', '/app/historico')
  })

  it('exibe exclusivamente os indicadores, posições e parcelas oficiais formatados', async () => {
    setup(); expect(screen.getByRole('status')).toHaveTextContent('Carregando dashboard')
    expect((await screen.findAllByText('R$ 1.250,30')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('R$ 1.000,10').length).toBeGreaterThan(0); expect(screen.getAllByText('R$ 250,20').length).toBeGreaterThan(1)
    expect(screen.getByRole('img', { name: /gráfico de pizza/i })).toBeInTheDocument()
    expect(screen.getByText('Saldo em carteira')).toBeInTheDocument(); expect(screen.getAllByText('Ações nacionais').length).toBeGreaterThan(0); expect(screen.getAllByText('Ações internacionais').length).toBeGreaterThan(0)
    expect(screen.getAllByText('PETR4').length).toBeGreaterThan(0); expect(screen.getAllByText('Compartilhado pela conta').length).toBeGreaterThan(0)
    expect(screen.getByText('80,0%')).toBeInTheDocument(); expect(screen.getByText('20,0%')).toBeInTheDocument()
  })

  it('consulta novamente ao trocar corretora, oculta resposta anterior e mantém saldo compartilhado oficial', async () => {
    setup(); await screen.findAllByText('PETR4')
    const pending = deferred(); getDashboard.mockReturnValueOnce(pending.promise)
    fireEvent.change(screen.getByLabelText('Visão'), { target: { value: 'broker-1' } })
    expect(screen.queryByText('PETR4')).not.toBeInTheDocument(); expect(screen.getByRole('status')).toHaveTextContent('Carregando')
    pending.resolve({ ...complete, selectedBrokerAssociationId: 'broker-1', positions: [{ ...positions[0], ticker: 'VALE3' }] })
    expect(await screen.findByText('VALE3')).toBeInTheDocument(); expect(getDashboard).toHaveBeenLastCalledWith({ brokerAssociationId: 'broker-1', period: '4W' })
    expect(screen.getAllByText('Compartilhado pela conta').length).toBeGreaterThan(0); expect(screen.getAllByText('Corretora Um').length).toBeGreaterThan(0)
  })

  it('oferece todos os períodos e apresenta somente os pontos devolvidos na ordem recebida', async () => {
    setup(); await screen.findAllByText('PETR4')
    for (const label of ['4 semanas', '3 meses', '6 meses', '1 ano', '5 anos', 'Máximo']) expect(screen.getByRole('radio', { name: new RegExp(label) })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('radio', { name: /3 meses/ }))
    await waitFor(() => expect(getDashboard).toHaveBeenLastCalledWith({ brokerAssociationId: '', period: '3M' }))
    const history = screen.getByRole('heading', { name: 'Histórico do patrimônio' }).closest('section')
    expect(within(history).getAllByRole('listitem')).toHaveLength(2)
    expect(within(history).getAllByRole('listitem')[0]).toHaveTextContent('R$ 1.100,11')
  })

  it('distingue vazios de posições, distribuições e histórico sem criar parcelas ou pontos', async () => {
    getDashboard.mockResolvedValue({ ...complete, positions: [], distributions: { byAsset: [], byBroker: [], byMarket: [] }, patrimonyHistory: [] })
    setup(); expect(await screen.findByText('Sua carteira ainda está vazia')).toBeInTheDocument()
    expect(screen.getAllByText('Sem parcelas')).toHaveLength(3); expect(screen.getByText('Sem pontos neste período')).toBeInTheDocument()
    expect(document.querySelectorAll('.dashboard-distribution li')).toHaveLength(0); expect(document.querySelectorAll('.dashboard-history li')).toHaveLength(0)
  })

  it('mostra avisos e instantes em Brasília sem ocultar valores confirmados', async () => {
    getDashboard.mockResolvedValue({ ...complete, positions: [{ ...positions[0], quoteStale: true }], warnings: [{ type: 'STALE_QUOTE', ticker: 'PETR4', observedAt: '2026-09-08T13:00:00Z' }, { type: 'STALE_EXCHANGE_RATE', ticker: null, observedAt: '2026-09-08T13:00:00Z' }] })
    setup(); expect(await screen.findByText('Preço de fechamento')).toBeInTheDocument()
    expect(screen.getByText(/Última cotação observada em/)).toBeInTheDocument(); expect(screen.queryByText('Dados desatualizados')).not.toBeInTheDocument(); expect(screen.getAllByText('R$ 1.250,30').length).toBeGreaterThan(0)
  })

  it('preserva filtros em erro recuperável e impede consulta duplicada equivalente', async () => {
    setup(); await screen.findAllByText('PETR4'); fireEvent.change(screen.getByLabelText('Visão'), { target: { value: 'broker-1' } }); await screen.findAllByText('PETR4')
    const pending = deferred(); getDashboard.mockReturnValue(pending.promise)
    fireEvent.click(screen.getByRole('radio', { name: /6 meses/ })); fireEvent.click(screen.getByRole('radio', { name: /6 meses/ }))
    expect(getDashboard).toHaveBeenCalledTimes(3); pending.reject(new DashboardApiError('Dashboard temporariamente indisponível.'))
    expect(await screen.findByRole('alert')).toHaveTextContent('Dashboard temporariamente indisponível.')
    expect(screen.getByLabelText('Visão')).toHaveValue('broker-1'); expect(screen.getByRole('radio', { name: /6 meses/ })).toBeChecked()
  })

  it('descarta resposta obsoleta após uma nova seleção', async () => {
    setup(); await screen.findAllByText('PETR4'); const oldRequest = deferred(); const currentRequest = deferred(); getDashboard.mockReturnValueOnce(oldRequest.promise).mockReturnValueOnce(currentRequest.promise)
    fireEvent.change(screen.getByLabelText('Visão'), { target: { value: 'broker-1' } }); fireEvent.change(screen.getByLabelText('Visão'), { target: { value: 'broker-2' } })
    currentRequest.resolve({ ...complete, positions: [{ ...positions[0], ticker: 'AAPL' }] }); expect(await screen.findByText('AAPL')).toBeInTheDocument()
    oldRequest.resolve({ ...complete, positions: [{ ...positions[0], ticker: 'VALE3' }] }); await waitFor(() => expect(screen.queryByText('VALE3')).not.toBeInTheDocument())
  })

  it('limpa dados privados e navega ao login em 401', async () => {
    getDashboard.mockRejectedValue(new DashboardApiError('Sessão expirada.', {}, 401)); const auth = setup()
    expect(await screen.findByText('Login destino')).toBeInTheDocument(); expect(auth.clear).toHaveBeenCalled(); expect(screen.queryByText('PETR4')).not.toBeInTheDocument()
  })

  it.each([320, 768, 1280])('mantém seções e filtros acessíveis no viewport de %d px', async (width) => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: width }); setup(); await screen.findAllByText('PETR4')
    expect(screen.getByRole('heading', { name: 'Indicadores' })).toBeInTheDocument(); expect(screen.getByLabelText('Visão')).toBeInTheDocument(); expect(screen.getByRole('group', { name: 'Período do patrimônio' })).toBeInTheDocument()
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(document.documentElement.clientWidth)
  })
})

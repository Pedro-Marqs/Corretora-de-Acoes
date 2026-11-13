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
const complete = { availableBalanceBrl: '1000.10', balanceShared: true, selectedBrokerAssociationId: null, positions, positionsMarketValueBrl: '250.20', patrimonyBrl: '1250.30', realizedResultBrl: '10.40', unrealizedResultBrl: '50.50', totalResultBrl: '60.60', distributions: { byAsset: [{ identifier: 'PETR4', label: 'PETR4', valueBrl: '250.20' }], byBroker: [{ identifier: 'broker-1', label: 'Corretora Um', valueBrl: '250.20' }], byMarket: [{ identifier: 'BR', label: 'Brasil', valueBrl: '250.20' }] }, exchangeRate: null, warnings: [], period: '4W', patrimonyHistory: [{ recordedAt: '2026-08-20T10:00:00-03:00', patrimonyBrl: '1100.11' }, { recordedAt: '2026-09-01T10:00:00-03:00', patrimonyBrl: '1250.30' }], investmentHistory: [{ recordedAt: '2026-08-20T10:00:00-03:00', positionsValueBrl: '200.10' }, { recordedAt: '2026-09-01T10:00:00-03:00', positionsValueBrl: '250.20' }] }

function deferred() { let resolve; let reject; const promise = new Promise((done, fail) => { resolve = done; reject = fail }); return { promise, resolve, reject } }
function setup(auth = { clear: vi.fn() }) { render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/app/dashboard']}><Routes><Route path="/app/dashboard" element={<DashboardPage />} /><Route path="/login" element={<p>Login destino</p>} /></Routes></MemoryRouter></AuthContext.Provider>); return auth }
afterEach(() => { cleanup(); vi.useRealTimers() })

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
    expect(screen.getByText('Valor de mercado das posições')).toBeInTheDocument(); expect(screen.getAllByText('Percentual não fornecido pela API')).toHaveLength(3)
    expect(screen.getByRole('heading', { name: 'Evolução do valor investido' })).toBeInTheDocument(); expect(screen.getByText('Por ativo')).toBeInTheDocument(); expect(screen.getByText('Por corretora')).toBeInTheDocument(); expect(screen.getByText('Por mercado')).toBeInTheDocument()
  })

  it('consulta novamente ao trocar corretora, oculta resposta anterior e mantém saldo compartilhado oficial', async () => {
    setup(); await screen.findAllByText('PETR4')
    const pending = deferred(); getDashboard.mockReturnValueOnce(pending.promise)
    fireEvent.change(screen.getByLabelText('Visão'), { target: { value: 'broker-1' } })
    expect(screen.getAllByText('PETR4').length).toBeGreaterThan(0); expect(screen.getByRole('status')).toHaveTextContent('Atualizando dashboard')
    pending.resolve({ ...complete, selectedBrokerAssociationId: 'broker-1', positions: [{ ...positions[0], ticker: 'VALE3' }] })
    expect(await screen.findByText('VALE3')).toBeInTheDocument(); expect(getDashboard).toHaveBeenLastCalledWith({ brokerAssociationId: 'broker-1', period: '4W' })
    expect(screen.getAllByText('Compartilhado pela conta').length).toBeGreaterThan(0); expect(screen.getAllByText('Corretora Um').length).toBeGreaterThan(0)
  })

  it('oferece todos os períodos e apresenta o crescimento patrimonial na ordem cronológica', async () => {
    setup(); await screen.findAllByText('PETR4')
    for (const label of ['4 semanas', '3 meses', '6 meses', '1 ano', '5 anos', 'Máximo']) expect(screen.getByRole('radio', { name: new RegExp(label) })).toBeInTheDocument()
    getDashboard.mockResolvedValueOnce({ ...complete, period: '3M' })
    fireEvent.click(screen.getByRole('radio', { name: /3 meses/ }))
    await waitFor(() => expect(getDashboard).toHaveBeenLastCalledWith({ brokerAssociationId: '', period: '3M' }))
    const history = screen.getByRole('heading', { name: 'Crescimento patrimonial' }).closest('section')
    expect(within(history).getAllByRole('listitem')).toHaveLength(2)
    expect(within(history).getAllByRole('listitem')[0]).toHaveTextContent('R$ 1.100,11')
    expect(within(history).queryByText('R$ 0,00')).not.toBeInTheDocument()
    expect(within(history).getByText('Resultado não realizado')).toBeInTheDocument()
    expect(within(history).getByText('R$ 50,50')).toBeInTheDocument()
    expect(within(history).queryByText(/Variação no período/)).not.toBeInTheDocument()
  })

  it('mantém o gráfico visível quando o patrimônio permanece igual no período', async () => {
    getDashboard.mockResolvedValue({
      ...complete,
      investmentHistory: [],
      patrimonyHistory: [
        { recordedAt: '2026-08-20T10:00:00-03:00', patrimonyBrl: '1250.30' },
        { recordedAt: '2026-09-01T10:00:00-03:00', patrimonyBrl: '1250.30' },
      ],
    })
    setup()
    await screen.findAllByText('PETR4')
    const history = screen.getByRole('heading', { name: 'Crescimento patrimonial' }).closest('section')
    expect(within(history).queryByText('Sem pontos neste período')).not.toBeInTheDocument()
    expect(within(history).getByRole('group', { name: /gráfico de linha do crescimento patrimonial/i })).toBeInTheDocument()
    expect(within(history).getAllByRole('listitem')).toHaveLength(2)
  })

  it('preserva o timestamp oficial sem criar âncoras de janela ou ponto atual', async () => {
    const historicalPoint = new Date(Date.now() - 56 * 24 * 60 * 60 * 1000).toISOString()
    getDashboard.mockResolvedValue({
      ...complete,
      patrimonyBrl: '8000.00',
      patrimonyHistory: [{ recordedAt: historicalPoint, patrimonyBrl: '8000.00' }],
    })
    setup(); await screen.findAllByText('PETR4')
    const history = screen.getByRole('heading', { name: 'Crescimento patrimonial' }).closest('section')
    const chart = within(history).getByRole('group', { name: /gráfico de linha do crescimento patrimonial/i })
    expect(within(chart).getAllByRole('button')).toHaveLength(1)
    expect(within(chart).getAllByRole('button')[0]).toHaveAccessibleName(new RegExp(new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(historicalPoint))))
    expect(chart.querySelector('.line-chart-line').getAttribute('points').split(' ')).toHaveLength(2)
    expect(chart).not.toHaveTextContent(/início do período|Patrimônio atual/)
    expect(within(history).getAllByRole('listitem')).toHaveLength(1)
  })

  it('oferece área em gradiente e tooltip navegável pelos pontos oficiais', async () => {
    setup(); await screen.findAllByText('PETR4')
    const history = screen.getByRole('heading', { name: 'Crescimento patrimonial' }).closest('section')
    const chart = within(history).getByRole('group', { name: /gráfico de linha do crescimento patrimonial/i })
    expect(chart.querySelector('.line-chart-area')).toBeInTheDocument()
    const points = within(chart).getAllByRole('button')
    expect(points).toHaveLength(2)
    points[0].focus()
    fireEvent.focus(points[0])
    expect(points[0]).toHaveFocus()
    expect(chart.querySelector('.line-chart-tooltip')).toHaveTextContent('R$ 1.100,11')
    fireEvent.keyDown(points[0], { key: 'ArrowRight' })
    expect(chart.querySelector('.line-chart-tooltip')).toHaveTextContent('R$ 1.250,30')
  })

  it('distingue carteira vazia e histórico sem criar parcelas ou pontos', async () => {
    getDashboard.mockResolvedValue({ ...complete, positions: [], distributions: { byAsset: [], byBroker: [], byMarket: [] }, patrimonyHistory: [] })
    setup(); expect(await screen.findByText('Sua carteira ainda está vazia')).toBeInTheDocument()
    expect(screen.getByText('Sem pontos neste período')).toBeInTheDocument()
    expect(screen.getAllByRole('status')).toHaveLength(3); expect(screen.getAllByText('Nenhuma distribuição oficial foi retornada.')).toHaveLength(3); expect(document.querySelectorAll('.dashboard-history li')).toHaveLength(0)
  })

  it('exibe avisos de cotação e câmbio sem apresentar valores desatualizados como atuais', async () => {
    getDashboard.mockResolvedValue({ ...complete, positions: [{ ...positions[0], quoteStale: true }], warnings: [{ type: 'STALE_QUOTE', ticker: 'PETR4', observedAt: '2026-09-08T13:00:00Z' }, { type: 'STALE_EXCHANGE_RATE', ticker: null, observedAt: '2026-09-08T13:00:00Z' }] })
    setup(); await screen.findAllByText('PETR4')
    expect(screen.queryByText('Preço de fechamento')).not.toBeInTheDocument()
    expect(screen.getByText(/A cotação de PETR4 está desatualizada/)).toBeInTheDocument(); expect(screen.getByText(/A cotação USD\/BRL está desatualizada/)).toBeInTheDocument(); expect(screen.getAllByText(/Referência:/).length).toBeGreaterThanOrEqual(3); expect(screen.getByText(/Última cotação:/)).toBeInTheDocument(); expect(screen.queryByText('Preço atual')).not.toBeInTheDocument()
  })

  it('identifica a cotação internacional em USD e mantém o preço médio oficial em BRL', async () => {
    const usPosition = { ...positions[0], ticker: 'AAPL', name: 'Apple', market: 'US', currency: 'USD', brokerageName: 'Corretora Um', quotePrice: 100, quotePriceBrl: 550, averagePriceBrl: 500, marketValueBrl: 550, unrealizedResultBrl: 50 }
    getDashboard.mockResolvedValue({ ...complete, positions: [usPosition], distributions: { byAsset: [{ identifier: 'AAPL', label: 'AAPL', valueBrl: '550' }], byBroker: complete.distributions.byBroker, byMarket: [{ identifier: 'US', label: 'Exterior', valueBrl: '550' }] }, exchangeRate: { currencyPair: 'USD/BRL', rate: '5.50', quotedAt: '2026-09-08T13:00:00Z', stale: false } })
    setup(); await screen.findAllByText('AAPL')
    expect(screen.getByRole('columnheader', { name: 'Preço médio (BRL)' })).toBeInTheDocument(); expect(screen.getByText(/US\$ 100,00/)).toBeInTheDocument(); expect(screen.getByText('R$ 500,00')).toBeInTheDocument(); expect(screen.getByText('BRL: R$ 550,00')).toBeInTheDocument()
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

  it('cobre todos os períodos suportados e recupera falha ao carregar corretoras', async () => {
    getActiveBrokers.mockRejectedValueOnce(new Error('Corretoras indisponíveis.')).mockResolvedValueOnce(brokers)
    setup()
    expect(await screen.findByText('Corretoras indisponíveis.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar carregar corretoras novamente' }))
    expect(await screen.findByText('Corretora Um')).toBeInTheDocument()
    await screen.findAllByText('PETR4')
    getDashboard.mockImplementation(({ period }) => Promise.resolve({ ...complete, period }))
    for (const period of ['6M', '1Y', '5Y', 'MAX']) {
      fireEvent.click(screen.getByRole('radio', { name: new RegExp(period) }))
      await waitFor(() => expect(getDashboard).toHaveBeenLastCalledWith({ brokerAssociationId: '', period }))
    }
  })

  it('mantém cada posição oficial sem consolidar corretoras nem recalcular resultados', async () => {
    const manyPositions = [
      positions[0],
      { ...positions[0], brokerageName: 'Corretora Dois', totalCostBrl: 300, marketValueBrl: 350, unrealizedResultBrl: 100 },
      ...Array.from({ length: 5 }, (_, index) => ({ ...positions[0], ticker: `PETR${index + 5}`, totalCostBrl: 200, marketValueBrl: 250, unrealizedResultBrl: 50 })),
    ]
    getDashboard.mockResolvedValue({ ...complete, positions: manyPositions, distributions: { ...complete.distributions, byMarket: [{ identifier: 'BR', label: 'Brasil', valueBrl: '1500' }] } })
    setup()
    await screen.findByText('Ver lista completa de ações')
    expect(screen.getAllByText('PETR4').length).toBeGreaterThanOrEqual(3)
    expect(screen.queryByText('Várias corretoras')).not.toBeInTheDocument()
    expect(screen.getByText('Sem dados')).toBeInTheDocument()
    const nationalCard = screen.getAllByRole('button').find((button) => button.textContent.includes('nacionais'))
    fireEvent.click(nationalCard)
    expect(screen.getByRole('button', { name: 'Ver toda a carteira' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ver toda a carteira' }))
    expect(screen.queryByRole('button', { name: 'Ver toda a carteira' })).not.toBeInTheDocument()
  })

  it('consolida as observacoes do mesmo dia usando o fechamento oficial', async () => {
    getDashboard.mockResolvedValue({
      ...complete,
      patrimonyHistory: [
        { recordedAt: '2026-09-09T11:15:00-03:00', patrimonyBrl: '10000.00' },
        { recordedAt: '2026-09-09T15:07:00-03:00', patrimonyBrl: '10100.00' },
        { recordedAt: '2026-09-09T15:08:00-03:00', patrimonyBrl: '10100.00' },
        { recordedAt: '2026-09-09T15:10:00-03:00', patrimonyBrl: '10099.80' },
        { recordedAt: '2026-09-23T09:26:00-03:00', patrimonyBrl: '10322.95' },
      ],
    })
    setup(); await screen.findAllByText('PETR4')
    const history = screen.getByRole('heading', { name: 'Crescimento patrimonial' }).closest('section')
    const chart = within(history).getByRole('group', { name: /gráfico de linha do crescimento patrimonial/i })
    expect(within(history).getByText('2 pontos oficiais')).toBeInTheDocument()
    expect(within(chart).getAllByRole('button')).toHaveLength(2)
    expect(within(chart).getAllByRole('button')[0]).toHaveAccessibleName(/10\.099,80/)
    expect(within(chart).getAllByRole('button')[1]).toHaveAccessibleName(/10\.322,95/)
    expect(within(history).getAllByRole('listitem')).toHaveLength(2)
    expect(within(history).getAllByRole('listitem')[0]).toHaveTextContent('R$ 10.099,80')
    expect(within(history).queryByText('R$ 10.000,00')).not.toBeInTheDocument()
  })

  it.each([320, 390, 768, 1366, 1440])('mantém seções e filtros acessíveis no viewport de %d px', async (width) => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: width }); setup(); await screen.findAllByText('PETR4')
    expect(screen.getByRole('heading', { name: 'Indicadores' })).toBeInTheDocument(); expect(screen.getByLabelText('Visão')).toBeInTheDocument(); expect(screen.getByRole('group', { name: 'Período do patrimônio' })).toBeInTheDocument(); expect(document.querySelector('.line-chart')).toBeInTheDocument()
  })

  it('mantém coordenadas SVG finitas para ponto único, valor negativo e timestamps repetidos', async () => {
    getDashboard.mockResolvedValue({ ...complete, patrimonyHistory: [{ recordedAt: '2026-09-01T10:00:00Z', patrimonyBrl: '-10' }, { recordedAt: '2026-09-01T10:00:00Z', patrimonyBrl: '-10' }] })
    setup(); await screen.findAllByText('PETR4')
    const chart = screen.getByRole('group', { name: /gráfico de linha do crescimento patrimonial/i })
    const values = [...chart.querySelectorAll('.line-chart-line, .line-chart-area, .line-chart-point')].flatMap((element) => [...element.attributes].map((attribute) => attribute.value)).join(' ')
    expect(values).not.toMatch(/NaN|Infinity/); expect(within(chart).getAllByRole('button')).toHaveLength(1)
  })
})

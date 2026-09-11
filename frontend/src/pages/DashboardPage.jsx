import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { getDashboard } from '../api/dashboard.js'
import { EmptyState, ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

const PERIODS = [{ value: '4W', label: '4 semanas' }, { value: '3M', label: '3 meses' }, { value: '6M', label: '6 meses' }, { value: '1Y', label: '1 ano' }, { value: '5Y', label: '5 anos' }, { value: 'MAX', label: 'Máximo' }]

export default function DashboardPage() {
  const { clear } = useAuth(); const navigate = useNavigate()
  const sequence = useRef(0); const brokerSequence = useRef(0); const activeQueries = useRef(new Set())
  const [filters, setFilters] = useState({ brokerAssociationId: '', period: '4W' })
  const [state, setState] = useState({ status: 'loading', data: null, message: '' })
  const [brokers, setBrokers] = useState([]); const [brokerError, setBrokerError] = useState('')
  const expireSession = useCallback(() => { sequence.current += 1; brokerSequence.current += 1; setState({ status: 'unauthenticated', data: null, message: '' }); clear(); navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } }) }, [clear, navigate])
  const load = useCallback(async (selection) => {
    const key = JSON.stringify(selection); if (activeQueries.current.has(key)) return
    activeQueries.current.add(key); const request = ++sequence.current; setState({ status: 'loading', data: null, message: '' })
    try { const data = await getDashboard(selection); if (request === sequence.current) setState({ status: 'ready', data, message: '' }) }
    catch (error) { if (request !== sequence.current) return; if (error?.status === 401) expireSession(); else setState({ status: 'error', data: null, message: error?.message || 'Não foi possível consultar o dashboard.' }) }
    finally { activeQueries.current.delete(key) }
  }, [expireSession])
  const loadBrokers = useCallback(async () => {
    const request = ++brokerSequence.current; setBrokerError('')
    try { const items = await getActiveBrokers(); if (request === brokerSequence.current) setBrokers(items) }
    catch (error) { if (request !== brokerSequence.current) return; if (error?.status === 401) expireSession(); else setBrokerError(error?.message || 'Não foi possível carregar as corretoras.') }
  }, [expireSession])
  useEffect(() => { const initialLoad = window.setTimeout(() => { load(filters); loadBrokers() }, 0); return () => { window.clearTimeout(initialLoad); sequence.current += 1; brokerSequence.current += 1 } }, []) // eslint-disable-line react-hooks/exhaustive-deps
  function selectFilter(event) { const next = { ...filters, [event.target.name]: event.target.value }; setFilters(next); load(next) }
  return <main className="dashboard-page">
    <header className="dashboard-heading"><p className="eyebrow">Visão consolidada</p><h1>Investimentos</h1><p>Acompanhe os valores oficiais da sua conta e das suas posições.</p></header>
    <nav className="investment-shortcuts" aria-label="Ações de investimentos"><Link to="/app/transferencias">Transferências</Link><Link to="/app/historico">Histórico</Link></nav>
    <section className="dashboard-filters" aria-labelledby="dashboard-filters-title"><h2 id="dashboard-filters-title">Filtros do dashboard</h2><div className="form-field"><label htmlFor="dashboard-view">Visão</label><select id="dashboard-view" name="brokerAssociationId" value={filters.brokerAssociationId} onChange={selectFilter} disabled={state.status === 'loading'}><option value="">Visão geral</option>{brokers.map((broker) => <option key={broker.associationId} value={broker.associationId}>{broker.tradeName}</option>)}</select></div><fieldset><legend>Período do patrimônio</legend><div className="dashboard-periods">{PERIODS.map((period) => <label key={period.value}><input type="radio" name="period" value={period.value} checked={filters.period === period.value} onChange={selectFilter} disabled={state.status === 'loading'} /><span>{period.value}<small>{period.label}</small></span></label>)}</div></fieldset></section>
    {brokerError && <Message kind="error"><p>{brokerError}</p><button className="secondary-button" type="button" onClick={loadBrokers}>Tentar carregar corretoras novamente</button></Message>}
    <section className="dashboard-content" aria-live="polite" aria-busy={state.status === 'loading'}>{state.status === 'loading' && <LoadingState message="Carregando dashboard…" />}{state.status === 'error' && <ErrorState message={state.message} onRetry={() => load(filters)} />}{state.status === 'ready' && <DashboardContent data={state.data} selectedBroker={brokers.find((broker) => broker.associationId === filters.brokerAssociationId)} />}</section>
  </main>
}

function DashboardContent({ data, selectedBroker }) {
  const [market, setMarket] = useState(''); const navigate = useNavigate()
  const positions = aggregatePositions(data.positions)
  const selectedPositions = market ? positions.filter((position) => position.market === market) : positions
  return <><section className="dashboard-context" aria-label="Contexto da consulta"><strong>{selectedBroker ? selectedBroker.tradeName : 'Todas as corretoras'}</strong><span>Período: {data.period}</span><span>Dados confirmados pelo servidor</span></section>
    <section aria-labelledby="dashboard-indicators-title"><h2 id="dashboard-indicators-title" className="section-title">Indicadores</h2><div className="dashboard-indicators dashboard-indicators-primary"><Indicator label="Total investido" value={data.positionsMarketValueBrl} note="Valor atual das posições" featured /><Indicator label="Saldo em conta" value={data.availableBalanceBrl} note="Compartilhado pela conta" /><Indicator label="Patrimônio total" value={data.patrimonyBrl} note="Investimentos + saldo" /><Indicator label="Resultado realizado" value={data.realizedResultBrl} /><Indicator label="Valorização não realizada" value={data.unrealizedResultBrl} /><Indicator label="Resultado total" value={data.totalResultBrl} /></div></section>
    <History points={(data.investmentHistory ?? data.patrimonyHistory).map((point) => ({ recordedAt: point.recordedAt, patrimonyBrl: point.positionsValueBrl ?? point.patrimonyBrl }))} period={data.period} />
    <AllocationPie balance={data.availableBalanceBrl} marketSlices={data.distributions.byMarket} />
    <section aria-labelledby="investment-allocation-title"><h2 id="investment-allocation-title" className="section-title">Distribuição por mercado</h2><div className="investment-market-cards"><MarketCard label="Ações nacionais" market="BR" slices={data.distributions.byMarket} selected={market === 'BR'} onSelect={setMarket} /><MarketCard label="Ações internacionais" market="US" slices={data.distributions.byMarket} selected={market === 'US'} onSelect={setMarket} /><article className="investment-market-card"><span>Saldo em conta</span><strong>{formatCurrency(data.availableBalanceBrl)}</strong><small>Valor oficial compartilhado</small></article></div></section>
    <section className="investment-wallet" aria-labelledby="investment-wallet-title"><header><div><h2 id="investment-wallet-title" className="section-title">Carteira</h2><p>{market ? 'Posições de ' + (market === 'BR' ? 'ações nacionais' : 'ações internacionais') : 'Todas as posições abertas'}</p></div>{market && <button className="text-button" type="button" onClick={() => setMarket('')}>Ver toda a carteira</button>}</header>{selectedPositions.length === 0 ? <EmptyState title={market ? 'Nenhuma posição nesta categoria' : 'Sua carteira ainda está vazia'} description="Nenhuma posição oficial foi retornada para esta seleção." /> : <DashboardPositionTable positions={selectedPositions} exchangeRate={data.exchangeRate} onInvest={(position) => navigate('/app/bolsa/' + position.market + '/' + position.ticker)} />}</section>
  </>
}

function AllocationPie({ balance, marketSlices }) {
  const slices = [
    { identifier: 'balance', label: 'Saldo em carteira', value: numeric(balance), color: '#70dec2' },
    { identifier: 'BR', label: 'Ações nacionais', value: marketTotal(marketSlices, 'BR'), color: '#008f7a' },
    { identifier: 'US', label: 'Ações internacionais', value: marketTotal(marketSlices, 'US'), color: '#2bc3a2' },
  ]
  const total = slices.reduce((sum, slice) => sum + slice.value, 0)
  let cursor = 0
  const gradient = total > 0 ? `conic-gradient(${slices.map((slice) => { const start = cursor / total * 360; cursor += slice.value; return `${slice.color} ${start}deg ${cursor / total * 360}deg` }).join(', ')})` : '#343a38'
  return <section className="allocation-overview" aria-labelledby="allocation-overview-title"><header><div><p className="eyebrow">Visão patrimonial</p><h2 id="allocation-overview-title">Distribuição do patrimônio</h2></div></header><div className="allocation-pie-layout"><div className="allocation-pie-stack"><strong className="allocation-pie-total">{formatCurrency(total)}</strong><small className="allocation-pie-caption">Total distribuído</small><div className="allocation-pie" role="img" aria-label="Gráfico de pizza com saldo em carteira, ações nacionais e ações internacionais" style={{ background: gradient }}><div aria-hidden="true" /></div></div><ul className="allocation-legend">{slices.map((slice) => <li key={slice.identifier}><span className="allocation-dot" style={{ background: slice.color }} /><span>{slice.label}</span><strong>{formatCurrency(slice.value)}</strong><small>{total > 0 ? `${(slice.value / total * 100).toFixed(1).replace('.', ',')}%` : '0,0%'}</small></li>)}</ul></div></section>
}

function numeric(value) { const number = Number(value); return Number.isFinite(number) && number > 0 ? number : 0 }
function marketTotal(slices, market) { return (slices ?? []).filter((slice) => String(slice.identifier).toUpperCase() === market).reduce((sum, slice) => sum + numeric(slice.valueBrl), 0) }
function Indicator({ label, value, note, featured = false }) { return <article className={'dashboard-indicator' + (featured ? ' dashboard-indicator-featured' : '')}><span>{label}</span><strong>{formatCurrency(value)}</strong>{note && <small>{note}</small>}</article> }
function MarketCard({ label, market, slices, selected, onSelect }) { const official = slices.find((slice) => slice.identifier === market); return <button className={'investment-market-card' + (selected ? ' selected' : '')} type="button" aria-pressed={selected} onClick={() => onSelect(market)}><span>{label}</span><strong>{official ? formatCurrency(official.valueBrl) : 'Sem dados'}</strong><small>Abrir posições</small></button> }
function DashboardPositionTable({ positions, exchangeRate, onInvest }) { const visiblePositions = positions.length > 5 ? positions.slice(0, 5) : positions; return <div className="dashboard-positions-table" role="table" aria-label="Posições da carteira"><div className="dashboard-positions-head" role="row"><span role="columnheader">Ticker</span><span role="columnheader">Quantidade</span><span role="columnheader">Saldo / posição</span><span role="columnheader">Preço atual</span><span role="columnheader">Preço médio</span><span role="columnheader">Rentabilidade</span></div><ul>{visiblePositions.map((position, index) => <li key={position.ticker + '-' + position.brokerageName + '-' + index}><button className="dashboard-position-row" type="button" onClick={() => onInvest(position)}><span className="dashboard-position-cell dashboard-position-ticker"><strong>{position.ticker}</strong><small>{position.name} · {position.market} · {position.brokerageName}</small></span><span className="dashboard-position-cell"><strong>{position.quantity}</strong></span><span className="dashboard-position-cell"><strong>{formatCurrency(position.marketValueBrl)}</strong><small>Valor da posição</small></span><span className="dashboard-position-cell"><strong>{dashboardCurrentPrice(position)}</strong>{position.market === 'US' && <small>({formatCurrency(position.quotePriceBrl)})</small>}</span><span className="dashboard-position-cell"><strong>{dashboardAveragePrice(position, exchangeRate)}</strong>{position.market === 'US' && <small>({formatCurrency(position.averagePriceBrl)})</small>}</span><span className={'dashboard-position-cell ' + (Number(position.unrealizedResultBrl) >= 0 ? 'positive-value' : 'negative-value')}><strong>{formatCurrency(position.unrealizedResultBrl)}</strong><small>{dashboardProfitability(position)}</small></span></button></li>)}</ul>{positions.length > 5 && <Link className="positions-more-link" to="/app/operacoes">Ver lista completa de ações <span aria-hidden="true">→</span></Link>}</div> }
function aggregatePositions(positions) {
  const groups = new Map()
  for (const position of positions ?? []) {
    const key = `${position.market}:${position.ticker}`
    const current = groups.get(key)
    if (!current) {
      groups.set(key, { ...position, brokerPositions: [position] })
      continue
    }
    const quantity = Number(current.quantity) + Number(position.quantity)
    const totalCostBrl = Number(current.totalCostBrl) + Number(position.totalCostBrl)
    groups.set(key, {
      ...current,
      quantity,
      totalCostBrl,
      averagePriceBrl: quantity > 0 ? totalCostBrl / quantity : current.averagePriceBrl,
      marketValueBrl: Number(current.marketValueBrl) + Number(position.marketValueBrl),
      unrealizedResultBrl: Number(current.unrealizedResultBrl) + Number(position.unrealizedResultBrl),
      brokerageName: 'Várias corretoras',
      brokerPositions: [...current.brokerPositions, position],
    })
  }
  return [...groups.values()]
}
function dashboardCurrentPrice(position) { return position.quotePrice == null ? 'Indisponível' : position.market === 'US' ? formatMoney(position.quotePrice, 'USD') : formatCurrency(position.quotePriceBrl) }
function dashboardAveragePrice(position, exchangeRate) { if (position.averagePriceBrl == null) return 'Indisponível'; if (position.market !== 'US') return formatCurrency(position.averagePriceBrl); const rate = Number(exchangeRate?.rate); return rate > 0 ? formatMoney(Number(position.averagePriceBrl) / rate, 'USD') : 'Indisponível' }
function dashboardProfitability(position) { const result = Number(position.unrealizedResultBrl); const invested = Number(position.averagePriceBrl) * Number(position.quantity); return Number.isFinite(result) && Number.isFinite(invested) && invested > 0 ? `${(result / invested * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%` : 'Percentual indisponível' }
function History({ points, period }) {
  const orderedPoints = [...(points ?? [])].sort((left, right) => new Date(left.recordedAt) - new Date(right.recordedAt))
  const changedPoints = orderedPoints.filter((point, index) => Number(point.patrimonyBrl) > 0 && (index === 0 || Number(point.patrimonyBrl) !== Number(orderedPoints[index - 1].patrimonyBrl)))
  const firstValue = Number(changedPoints[0]?.patrimonyBrl)
  const lastValue = Number(changedPoints.at(-1)?.patrimonyBrl)
  const changeValue = Number.isFinite(firstValue) && Number.isFinite(lastValue) ? lastValue - firstValue : null
  const chartPoints = addPeriodStartPoint(changedPoints, period)
  const changeLabel = changeValue == null ? 'Resultado' : changeValue >= 0 ? 'Valorização' : 'Desvalorização'
  return <section className="dashboard-history" aria-labelledby="dashboard-history-title"><header className="dashboard-history-header"><div><p className="eyebrow">Série oficial</p><h2 id="dashboard-history-title">Crescimento patrimonial</h2><p>Crescimento apenas dos investimentos, desde a transação mais antiga, com oscilações das cotações.</p></div><span>{period}</span></header>{changedPoints.length === 0 ? <EmptyState title="Sem pontos neste período" description="Nenhuma posição foi registrada neste período." /> : <><div className="dashboard-history-body"><div className="line-chart-wrap"><LineChart points={chartPoints} /></div><aside className="history-summary" aria-label="Resumo da evolução"><dl><div><dt>Valor atual</dt><dd>{formatCurrency(lastValue)}</dd></div><div><dt>{changeLabel}</dt><dd className={changeValue == null || changeValue >= 0 ? 'positive-value' : 'negative-value'}>{changeValue == null ? 'Valor indisponível' : formatCurrency(Math.abs(changeValue))}</dd></div><div><dt>Período consultado</dt><dd>{period}</dd></div></dl><p className="history-summary-note">Resultado entre o primeiro e o último ponto oficial do período.</p></aside></div><details className="history-data"><summary>Ver dados da evolução</summary><ol>{chartPoints.map((point, index) => <li key={point.recordedAt + '-' + index}><time dateTime={point.recordedAt}>{formatBrasiliaDateTime(point.recordedAt)}</time><strong>{formatCurrency(point.patrimonyBrl)}</strong></li>)}</ol></details></>}</section>
}
function addPeriodStartPoint(points, period) {
  if (!points.length || period === 'MAX') return points
  const start = new Date()
  if (period === '4W') start.setDate(start.getDate() - 28)
  else if (period === '3M') start.setMonth(start.getMonth() - 3)
  else if (period === '6M') start.setMonth(start.getMonth() - 6)
  else if (period === '1Y') start.setFullYear(start.getFullYear() - 1)
  else if (period === '5Y') start.setFullYear(start.getFullYear() - 5)
  else return points
  start.setHours(0, 0, 0, 0)
  const firstPoint = new Date(points[0].recordedAt)
  return start < firstPoint ? [{ recordedAt: start.toISOString(), patrimonyBrl: 0 }, ...points] : points
}
function LineChart({ points }) {
  const values = points.map((point) => Number(point.patrimonyBrl)); const rawMin = Math.min(...values); const rawMax = Math.max(...values); const rawSpan = rawMax - rawMin; const paddingScale = rawSpan || Math.max(rawMax * .1, 1); const min = Math.max(0, rawMin - paddingScale * .08); const max = rawMax + paddingScale * .08; const span = max - min || 1
  const width = 820; const height = 290; const padding = { top: 18, right: 18, bottom: 50, left: 72 }; const plotWidth = width - padding.left - padding.right; const plotHeight = height - padding.top - padding.bottom; const tickCount = 5
  const timestamps = points.map((point) => new Date(point.recordedAt).getTime()); const firstTimestamp = Math.min(...timestamps); const lastTimestamp = Math.max(...timestamps); const timeSpan = lastTimestamp - firstTimestamp || 1
  const coordinates = points.map((point, index) => ({ x: padding.left + (points.length === 1 ? plotWidth / 2 : (timestamps[index] - firstTimestamp) / timeSpan * plotWidth), y: padding.top + (1 - (Number(point.patrimonyBrl) - min) / span) * plotHeight, ...point }))
  const line = coordinates.map(({ x, y }) => `${x},${y}`).join(' '); const ticks = Array.from({ length: tickCount }, (_, index) => max - span * index / (tickCount - 1)); const labelIndexes = new Set(points.length <= 5 ? points.map((_, index) => index) : [0, Math.floor((points.length - 1) / 2), points.length - 1])
  return <svg className="line-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gráfico de linha do crescimento patrimonial"><desc>Evolução oficial dos investimentos no período selecionado.</desc>{ticks.map((value, index) => { const y = padding.top + plotHeight * index / (tickCount - 1); return <g key={value}><line className="line-chart-grid" x1={padding.left} y1={y} x2={width - padding.right} y2={y} /><text className="line-chart-axis-label" x={padding.left - 12} y={y + 4} textAnchor="end">{formatChartCurrency(value)}</text></g> })}<polyline className="line-chart-line" points={line} fill="none" />{coordinates.map((point, index) => <g key={point.recordedAt}><title>{formatBrasiliaDateTime(point.recordedAt)} — {formatCurrency(point.patrimonyBrl)}</title><circle className="line-chart-point" cx={point.x} cy={point.y} r="5" />{labelIndexes.has(index) && <text className="line-chart-date" x={point.x} y={height - 14} textAnchor="middle">{formatChartDate(point.recordedAt)}</text>}</g>)}</svg>
}
function formatChartCurrency(value) { const number = Number(value); if (!Number.isFinite(number)) return '—'; if (Math.abs(number) >= 1000) return `R$ ${(number / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`; return formatCurrency(number) }
function formatChartDate(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? 'Data indisponível' : new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'America/Sao_Paulo' }).format(date) }

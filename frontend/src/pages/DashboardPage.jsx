import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { getDashboard } from '../api/dashboard.js'
import { EmptyState, ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

const PERIODS = [{ value: '4W', label: '4 semanas' }, { value: '3M', label: '3 meses' }, { value: '6M', label: '6 meses' }, { value: '1Y', label: '1 ano' }, { value: '5Y', label: '5 anos' }, { value: 'MAX', label: 'Máximo' }]
const DIMENSIONS = [{ field: 'byAsset', title: 'Por ativo' }, { field: 'byBroker', title: 'Por corretora' }, { field: 'byMarket', title: 'Por mercado' }]

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
  const selectedPositions = market ? data.positions.filter((position) => position.market === market) : data.positions
  return <><section className="dashboard-context" aria-label="Contexto da consulta"><strong>{selectedBroker ? selectedBroker.tradeName : 'Todas as corretoras'}</strong><span>Período: {data.period}</span><span>Dados confirmados pelo servidor</span></section>
    <AllocationPie balance={data.availableBalanceBrl} marketSlices={data.distributions.byMarket} />
    <section className="investment-overview" aria-labelledby="investment-overview-title"><h2 id="investment-overview-title" className="section-title">Resumo dos investimentos</h2><div className="investment-primary-values"><Indicator label="Total do patrimônio" value={data.patrimonyBrl} featured /><Indicator label="Saldo em conta" value={data.availableBalanceBrl} note="Compartilhado pela conta" /></div></section>
    <section aria-labelledby="investment-allocation-title"><h2 id="investment-allocation-title" className="section-title">Distribuição por mercado</h2><div className="investment-market-cards"><MarketCard label="Ações nacionais" market="BR" slices={data.distributions.byMarket} selected={market === 'BR'} onSelect={setMarket} /><MarketCard label="Ações internacionais" market="US" slices={data.distributions.byMarket} selected={market === 'US'} onSelect={setMarket} /><article className="investment-market-card"><span>Saldo em conta</span><strong>{formatCurrency(data.availableBalanceBrl)}</strong><small>Valor oficial compartilhado</small></article></div></section>
    <section className="investment-wallet" aria-labelledby="investment-wallet-title"><header><div><h2 id="investment-wallet-title" className="section-title">Carteira</h2><p>{market ? 'Posições de ' + (market === 'BR' ? 'ações nacionais' : 'ações internacionais') : 'Todas as posições abertas'}</p></div>{market && <button className="text-button" type="button" onClick={() => setMarket('')}>Ver toda a carteira</button>}</header>{selectedPositions.length === 0 ? <EmptyState title={market ? 'Nenhuma posição nesta categoria' : 'Sua carteira ainda está vazia'} description="Nenhuma posição oficial foi retornada para esta seleção." /> : <div className="dashboard-position-list">{selectedPositions.map((position, index) => <PositionCard key={position.ticker + '-' + position.brokerageName + '-' + index} position={position} onInvest={() => navigate('/app/bolsa/' + position.market + '/' + position.ticker)} />)}</div>}</section>
    <section aria-labelledby="dashboard-indicators-title"><h2 id="dashboard-indicators-title" className="section-title">Indicadores</h2><div className="dashboard-indicators"><Indicator label="Saldo disponível" value={data.availableBalanceBrl} note="Compartilhado pela conta" featured /><Indicator label="Patrimônio" value={data.patrimonyBrl} /><Indicator label="Valor das posições" value={data.positionsMarketValueBrl} /><Indicator label="Resultado realizado" value={data.realizedResultBrl} /><Indicator label="Valorização não realizada" value={data.unrealizedResultBrl} /><Indicator label="Resultado total" value={data.totalResultBrl} /></div></section>
    <section aria-labelledby="dashboard-distributions-title"><h2 id="dashboard-distributions-title" className="section-title">Distribuições detalhadas</h2><div className="dashboard-distributions">{DIMENSIONS.map(({ field, title }) => <Distribution key={field} title={title} slices={data.distributions[field]} />)}</div></section>
    <History points={data.patrimonyHistory} period={data.period} />
  </>
}

function AllocationPie({ balance, marketSlices }) {
  const slices = [
    { identifier: 'balance', label: 'Saldo em carteira', value: numeric(balance), color: '#42d8bb' },
    { identifier: 'BR', label: 'Ações nacionais', value: marketTotal(marketSlices, 'BR'), color: '#1aa894' },
    { identifier: 'US', label: 'Ações internacionais', value: marketTotal(marketSlices, 'US'), color: '#4779db' },
  ]
  const total = slices.reduce((sum, slice) => sum + slice.value, 0)
  let cursor = 0
  const gradient = total > 0 ? `conic-gradient(${slices.map((slice) => { const start = cursor / total * 360; cursor += slice.value; return `${slice.color} ${start}deg ${cursor / total * 360}deg` }).join(', ')})` : '#343a38'
  return <section className="allocation-overview" aria-labelledby="allocation-overview-title"><header><div><p className="eyebrow">Visão patrimonial</p><h2 id="allocation-overview-title">Distribuição do patrimônio</h2></div><span>{formatCurrency(total)}</span></header><div className="allocation-pie-layout"><div className="allocation-pie" role="img" aria-label="Gráfico de pizza com saldo em carteira, ações nacionais e ações internacionais" style={{ background: gradient }}><div><strong>{formatCurrency(total)}</strong><small>Total</small></div></div><ul className="allocation-legend">{slices.map((slice) => <li key={slice.identifier}><span className="allocation-dot" style={{ background: slice.color }} /><span>{slice.label}</span><strong>{formatCurrency(slice.value)}</strong><small>{total > 0 ? `${(slice.value / total * 100).toFixed(1).replace('.', ',')}%` : '0,0%'}</small></li>)}</ul></div></section>
}

function numeric(value) { const number = Number(value); return Number.isFinite(number) && number > 0 ? number : 0 }
function marketTotal(slices, market) { return (slices ?? []).filter((slice) => String(slice.identifier).toUpperCase() === market).reduce((sum, slice) => sum + numeric(slice.valueBrl), 0) }
function Indicator({ label, value, note, featured = false }) { return <article className={'dashboard-indicator' + (featured ? ' dashboard-indicator-featured' : '')}><span>{label}</span><strong>{formatCurrency(value)}</strong>{note && <small>{note}</small>}</article> }
function MarketCard({ label, market, slices, selected, onSelect }) { const official = slices.find((slice) => slice.identifier === market); return <button className={'investment-market-card' + (selected ? ' selected' : '')} type="button" aria-pressed={selected} onClick={() => onSelect(market)}><span>{label}</span><strong>{official ? formatCurrency(official.valueBrl) : 'Sem dados'}</strong><small>Abrir posições</small></button> }
function PositionCard({ position, onInvest }) { return <article className="dashboard-position"><header><div><strong>{position.ticker}</strong><span>{position.name}</span></div><span>{position.market} · {position.brokerageName}</span></header><dl><Detail label="Quantidade" value={position.quantity} /><Detail label="Preço médio" value={formatCurrency(position.averagePriceBrl)} /><Detail label="Cotação" value={formatMoney(position.quotePrice, position.currency)} /><Detail label="Cotação em reais" value={formatCurrency(position.quotePriceBrl)} /><Detail label="Valor de mercado" value={formatCurrency(position.marketValueBrl)} /><Detail label="Valorização" value={formatCurrency(position.unrealizedResultBrl)} /><Detail label="Cotação observada" value={formatBrasiliaDateTime(position.quoteQuotedAt)} /></dl>{position.quoteStale && <p className="position-stale" role="status"><strong>Preço de fechamento</strong><span>Última cotação observada em {formatBrasiliaDateTime(position.quoteQuotedAt)}</span></p>}<button className="secondary-button investment-action" type="button" onClick={onInvest}>Investir</button></article> }
function Distribution({ title, slices }) { return <section className="dashboard-distribution" aria-label={title}><h3>{title}</h3>{slices.length === 0 ? <EmptyState title="Sem parcelas" description="Não há posições para esta distribuição." /> : <><BarChart label={'Gráfico ' + title} slices={slices} /><ul>{slices.map((slice) => <li key={slice.identifier}><span>{slice.label}</span><strong>{formatCurrency(slice.valueBrl)}</strong></li>)}</ul></>}</section> }
function BarChart({ label, slices }) { const max = Math.max(...slices.map((slice) => Number(slice.valueBrl)), 0); return <div className="data-chart bar-chart" role="img" aria-label={label}>{slices.map((slice) => <div className="chart-row" key={slice.identifier}><span className="chart-label">{slice.label}</span><span className="chart-track"><span className="chart-bar" style={{ width: (max > 0 ? Math.max(Number(slice.valueBrl) / max * 100, 4) : 0) + '%' }} /></span><strong>{formatCurrency(slice.valueBrl)}</strong></div>)}</div> }
function History({ points, period }) { return <section className="dashboard-history" aria-labelledby="dashboard-history-title"><header><div><p className="eyebrow">Série oficial</p><h2 id="dashboard-history-title">Histórico do patrimônio</h2></div><span>{period}</span></header>{points.length === 0 ? <EmptyState title="Sem pontos neste período" description="Nenhum ponto patrimonial foi registrado para a seleção atual." /> : <><PatrimonyChart points={points} /><ol>{points.map((point, index) => <li key={point.recordedAt + '-' + index}><time dateTime={point.recordedAt}>{formatBrasiliaDateTime(point.recordedAt)}</time><strong>{formatCurrency(point.patrimonyBrl)}</strong></li>)}</ol></>}</section> }
function PatrimonyChart({ points }) { const values = points.map((point) => Number(point.patrimonyBrl)); const min = Math.min(...values); const max = Math.max(...values); const span = max - min || 1; return <div className="data-chart patrimony-chart" role="img" aria-label="Gráfico da evolução patrimonial">{points.map((point) => <div className="patrimony-point" key={point.recordedAt}><span className="chart-track"><span className="chart-bar" style={{ height: (24 + ((Number(point.patrimonyBrl) - min) / span) * 76) + '%' }} /></span><small>{formatBrasiliaDateTime(point.recordedAt)}</small><strong>{formatCurrency(point.patrimonyBrl)}</strong></div>)}</div> }
function Detail({ label, value }) { return <div><dt>{label}</dt><dd>{value}</dd></div> }

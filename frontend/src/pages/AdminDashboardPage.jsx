import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminDashboard } from '../api/admin-dashboard.js'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { EmptyState, ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'

const PERIODS = ['4W', '3M', '6M', '1Y', '5Y', 'MAX']
const numberFormat = new Intl.NumberFormat('pt-BR')
const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
function count(value) { return numberFormat.format(Number(value) || 0) }
function money(value, code = 'BRL') { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: code }).format(Number(value) || 0) }
function date(value) { return new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo' }).format(new Date(`${value}T12:00:00-03:00`)) }

export default function AdminDashboardPage() {
  const auth = useAdminAuth()
  const { clear } = auth
  const navigate = useNavigate()
  const [period, setPeriod] = useState('4W')
  const [reload, setReload] = useState(0)
  const [state, setState] = useState({ status: 'loading', data: null, message: '', loadedPeriod: '' })

  useEffect(() => {
    let active = true
    getAdminDashboard({ period, limit: 10 }).then((data) => {
      if (active) setState({ status: 'ready', data, message: '', loadedPeriod: period })
    }).catch((error) => {
      if (!active) return
      if (error.status === 401 || error.status === 403) { clear(); navigate('/admin/login', { replace: true }); return }
      setState({ status: 'error', data: null, message: error.message, loadedPeriod: period })
    })
    return () => { active = false }
  }, [period, reload, clear, navigate])

  const loading = state.status === 'loading' || state.loadedPeriod !== period
  return <section className="admin-page" aria-busy={loading}>
    <header className="admin-page-heading"><div><p className="eyebrow">Visão operacional</p><h1>Dashboard administrativo</h1><p>Indicadores agregados da corretora, sem exposição de dados individuais.</p></div><label className="admin-period-control">Período<select value={period} onChange={(event) => setPeriod(event.target.value)}>{PERIODS.map((item) => <option key={item}>{item}</option>)}</select></label></header>
    {loading && <LoadingState message="Carregando indicadores…" />}
    {!loading && state.status === 'error' && <ErrorState message={state.message} onRetry={() => setReload((current) => current + 1)} />}
    {!loading && state.status === 'ready' && <DashboardContent data={state.data} />}
  </section>
}

function DashboardContent({ data }) {
  const metrics = [
    ['Total atual de contas', data.totalAccounts], ['Contas ativas atualmente', data.activeAccounts], ['Contas inativas atualmente', data.inactiveAccounts], ['Novas contas no período', data.newAccounts],
    ['Compras', data.purchaseCount], ['Vendas', data.saleCount], ['Ações compradas', data.purchasedShares], ['Ações vendidas', data.soldShares],
  ]
  return <>
    <section className="admin-metric-grid" aria-label="Indicadores administrativos">{metrics.map(([label, value]) => <article className="admin-metric-card" key={label}><span>{label}</span><strong>{count(value)}</strong></article>)}</section>
    <section className="admin-panel-grid"><VolumeCard title="Volume de compras" volume={data.purchaseVolume} /><VolumeCard title="Volume de vendas" volume={data.saleVolume} /></section>
    <section className="admin-panel" aria-labelledby="admin-series-title"><header className="admin-panel-heading"><div><p className="eyebrow">Evolução diária</p><h2 id="admin-series-title">Série do período</h2></div></header>{data.series.length === 0 ? <EmptyState title="Sem movimentações no período" description="Não há contas ou negociações para exibir." /> : <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Data</th><th>Novas contas</th><th>Compras</th><th>Vendas</th></tr></thead><tbody>{data.series.map((item) => <tr key={item.date}><td>{date(item.date)}</td><td>{count(item.newAccounts)}</td><td>{count(item.purchaseCount)}</td><td>{count(item.saleCount)}</td></tr>)}</tbody></table></div>}</section>
    <section className="admin-panel" aria-labelledby="admin-ranking-title"><header className="admin-panel-heading"><div><p className="eyebrow">Ativos mais negociados</p><h2 id="admin-ranking-title">Ranking</h2></div></header>{data.ranking.length === 0 ? <EmptyState title="Sem ativos negociados" /> : <ol className="admin-ranking">{data.ranking.map((item, index) => <li key={`${item.market}-${item.ticker}`}><span>{index + 1}</span><strong>{item.ticker}</strong><small>{item.market}</small><b>{count(item.tradedShares)} ações</b></li>)}</ol>}</section>
  </>
}

function VolumeCard({ title, volume }) {
  const available = volume.conversionStatus === 'AVAILABLE'
  const notRequired = volume.conversionStatus === 'NOT_REQUIRED'
  return <article className="admin-panel admin-volume"><h2>{title}</h2><dl><div><dt>BRL</dt><dd>{money(volume.brl)}</dd></div><div><dt>USD</dt><dd>{money(volume.usd, 'USD')}</dd></div><div><dt>Equivalente BRL</dt><dd>{available ? currency.format(Number(volume.equivalentBrl)) : notRequired ? 'Não se aplica' : 'Indisponível'}</dd></div></dl>{available ? <small>Câmbio {Number(volume.usdBrlRate).toLocaleString('pt-BR')} · cotado em {new Date(volume.rateQuotedAt).toLocaleString('pt-BR')}</small> : notRequired ? <small>Não há volume em USD para converter neste período.</small> : <small className="admin-warning">Conversão USD→BRL indisponível para este período.</small>}</article>
}

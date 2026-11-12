import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminDashboard } from '../api/admin-dashboard.js'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { EmptyState, ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'
import { ResponsiveLineChart } from '../components/data-viz/index.jsx'
import { Badge, MetricCard, SectionHeader, SelectField, SurfaceCard } from '../components/ui/index.jsx'

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
    <SectionHeader
      eyebrow="Visão operacional"
      title="Dashboard administrativo"
      description="Indicadores agregados da corretora, sem exposição de dados individuais."
      actions={<SelectField className="admin-period-field" label="Período" value={period} onChange={(event) => setPeriod(event.target.value)}>
        {PERIODS.map((item) => <option key={item}>{item}</option>)}
      </SelectField>}
    />
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
  const seriesCharts = [
    { label: 'Novas contas', valueKey: 'newAccounts', description: 'Contas criadas por dia no período selecionado.' },
    { label: 'Compras', valueKey: 'purchaseCount', description: 'Compras confirmadas por dia no período selecionado.' },
    { label: 'Vendas', valueKey: 'saleCount', description: 'Vendas confirmadas por dia no período selecionado.' },
  ]
  return <>
    <section className="admin-metric-grid" aria-label="Indicadores administrativos">{metrics.map(([label, value]) => <MetricCard key={label} label={label} value={count(value)} />)}</section>
    <section className="admin-volume-grid" aria-label="Volumes financeiros"><VolumeCard title="Volume de compras" volume={data.purchaseVolume} /><VolumeCard title="Volume de vendas" volume={data.saleVolume} /></section>
    <SurfaceCard className="admin-panel admin-series-panel" aria-labelledby="admin-series-title">
      <header className="admin-panel-heading"><div><p className="admin-eyebrow">Evolução diária</p><h2 id="admin-series-title">Séries do período</h2><p>Somente os agregados oficiais retornados pela API administrativa.</p></div><Badge tone="info">{data.series.length} dias</Badge></header>
      {data.series.length === 0 ? <EmptyState title="Sem movimentações no período" description="Não há contas ou negociações para exibir." /> : <div className="admin-series-grid">{seriesCharts.map((chart) => <SurfaceCard key={chart.valueKey} tone="muted" className="admin-chart-card"><h3>{chart.label}</h3><ResponsiveLineChart data={data.series} label={chart.label} description={chart.description} period="Período selecionado" valueKey={chart.valueKey} labelKey="date" formatLabel={date} formatValue={count} /></SurfaceCard>)}</div>}
    </SurfaceCard>
    <SurfaceCard className="admin-panel admin-ranking-panel" aria-labelledby="admin-ranking-title">
      <header className="admin-panel-heading"><div><p className="admin-eyebrow">Ativos mais negociados</p><h2 id="admin-ranking-title">Ranking oficial</h2><p>Ordenação e limite definidos pelo contrato administrativo.</p></div><Badge tone="success">{data.ranking.length} ativos</Badge></header>
      {data.ranking.length === 0 ? <EmptyState title="Sem ativos negociados" /> : <ol className="admin-ranking-list">{data.ranking.map((item, index) => <li key={`${item.market}-${item.ticker}`}><span>{index + 1}</span><strong>{item.ticker}</strong><small>{item.market}</small><b>{count(item.tradedShares)} ações</b></li>)}</ol>}
    </SurfaceCard>
    <SurfaceCard className="admin-panel admin-series-table-panel" aria-labelledby="admin-series-table-title">
      <header className="admin-panel-heading"><div><p className="admin-eyebrow">Consulta acessível</p><h2 id="admin-series-table-title">Dados diários</h2></div></header>
      {data.series.length === 0 ? <EmptyState title="Sem dados diários" /> : <div className="admin-table-wrap"><table className="admin-table"><caption className="sr-only">Séries administrativas por data</caption><thead><tr><th scope="col">Data</th><th scope="col">Novas contas</th><th scope="col">Compras</th><th scope="col">Vendas</th></tr></thead><tbody>{data.series.map((item) => <tr key={item.date}><td>{date(item.date)}</td><td>{count(item.newAccounts)}</td><td>{count(item.purchaseCount)}</td><td>{count(item.saleCount)}</td></tr>)}</tbody></table></div>}
    </SurfaceCard>
  </>
}

function VolumeCard({ title, volume }) {
  const available = volume.conversionStatus === 'AVAILABLE'
  const notRequired = volume.conversionStatus === 'NOT_REQUIRED'
  return <SurfaceCard className="admin-panel admin-volume-card"><header className="admin-panel-heading"><div><p className="admin-eyebrow">Volume agregado</p><h2>{title}</h2></div><Badge tone={available ? 'success' : notRequired ? 'neutral' : 'warning'}>{available ? 'Conversão disponível' : notRequired ? 'Sem conversão' : 'Conversão indisponível'}</Badge></header><dl><div><dt>BRL nativo</dt><dd>{money(volume.brl)}</dd></div><div><dt>USD nativo</dt><dd>{money(volume.usd, 'USD')}</dd></div><div><dt>Equivalente BRL</dt><dd>{available ? currency.format(Number(volume.equivalentBrl)) : notRequired ? 'Não se aplica' : 'Indisponível'}</dd></div></dl>{available ? <small>Câmbio {Number(volume.usdBrlRate).toLocaleString('pt-BR')} · cotado em {new Date(volume.rateQuotedAt).toLocaleString('pt-BR')}</small> : notRequired ? <small>Não há volume em USD para converter neste período.</small> : <small className="admin-warning">Conversão USD→BRL indisponível para este período.</small>}</SurfaceCard>
}

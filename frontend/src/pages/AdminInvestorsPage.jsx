import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeAdminInvestorStatus, listAdminInvestors } from '../api/admin-investors.js'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { EmptyState, ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'

export default function AdminInvestorsPage() {
  const auth = useAdminAuth()
  const { clear } = auth
  const navigate = useNavigate()
  const [filters, setFilters] = useState({ status: 'ALL', search: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const [search, setSearch] = useState('')
  const [state, setState] = useState({ status: 'loading', data: null, message: '', loadedKey: '' })
  const [changingId, setChangingId] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    const key = JSON.stringify(filters)
    listAdminInvestors(filters).then((data) => { if (active) setState({ status: 'ready', data, message: '', loadedKey: key }) }).catch((error) => {
      if (!active) return
      if (error.status === 401 || error.status === 403) { clear(); navigate('/admin/login', { replace: true }); return }
      setState({ status: 'error', data: null, message: error.message, loadedKey: key })
    })
    return () => { active = false }
  }, [filters, clear, navigate])

  function updateFilter(event) { setFilters((current) => ({ ...current, [event.target.name]: event.target.value, page: 0 })) }
  function submitSearch(event) { event.preventDefault(); setFilters((current) => ({ ...current, search, page: 0 })) }
  function page(delta) { setFilters((current) => ({ ...current, page: Math.max(0, Math.min(current.page + delta, (state.data?.totalPages ?? 1) - 1)) })) }

  async function changeStatus(investor) {
    const nextStatus = investor.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const action = nextStatus === 'ACTIVE' ? 'ativar' : 'desativar'
    if (!window.confirm(`Confirma ${action} o investidor ${investor.name}?`)) return
    setChangingId(investor.id); setMessage('')
    try {
      await changeAdminInvestorStatus(investor.id, nextStatus)
      setMessage(`Investidor ${action === 'ativar' ? 'ativado' : 'desativado'} com sucesso.`)
      setFilters((current) => ({ ...current }))
    } catch (error) {
      if (error.status === 401 || error.status === 403) { clear(); navigate('/admin/login', { replace: true }) }
      else setMessage(error.status === 409 ? 'A conta já está nesse estado ou a transição não é válida.' : error.message)
    } finally { setChangingId(null) }
  }

  const loading = state.status === 'loading' || state.loadedKey !== JSON.stringify(filters)
  return <section className="admin-page"><header className="admin-page-heading"><div><p className="eyebrow">Contas da corretora</p><h1>Investidores</h1><p>Dados pessoais protegidos e status operacional de cada conta.</p></div></header>
    <section className="admin-panel admin-filters" aria-label="Filtros de investidores"><form onSubmit={submitSearch}><label htmlFor="admin-search">Buscar por nome ou e-mail<input id="admin-search" value={search} onChange={(event) => setSearch(event.target.value)} maxLength="100" placeholder="Digite para buscar" /></label><button className="secondary-button" type="submit">Buscar</button></form><label>Status<select name="status" value={filters.status} onChange={updateFilter}><option value="ALL">Todos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select></label><label>Ordenar<select name="sort" value={filters.sort} onChange={updateFilter}><option value="createdAt,desc">Mais recentes</option><option value="createdAt,asc">Mais antigos</option><option value="name,asc">Nome A–Z</option><option value="name,desc">Nome Z–A</option></select></label></section>
    {message && <div className="message admin-status-message" role="status">{message}</div>}
    {loading && <LoadingState message="Carregando investidores…" />}
    {!loading && state.status === 'error' && <ErrorState message={state.message} onRetry={() => setFilters((current) => ({ ...current }))} />}
    {!loading && state.status === 'ready' && <InvestorTable data={state.data} changingId={changingId} onChangeStatus={changeStatus} onPage={page} />}
  </section>
}

function InvestorTable({ data, changingId, onChangeStatus, onPage }) {
  return <section className="admin-panel"><div className="admin-table-wrap">{data.content.length === 0 ? <EmptyState title="Nenhum investidor encontrado" description="Ajuste os filtros e tente novamente." /> : <table className="admin-table"><thead><tr><th>Nome</th><th>E-mail</th><th>CPF</th><th>Status</th><th>Criado em</th><th>Ação</th></tr></thead><tbody>{data.content.map((investor) => <tr key={investor.id}><td>{investor.name}</td><td>{investor.emailMasked}</td><td>{investor.cpfMasked}</td><td><span className={`admin-status admin-status-${investor.status.toLowerCase()}`}>{investor.status === 'ACTIVE' ? 'Ativo' : 'Inativo'}</span></td><td>{formatDate(investor.createdAt)}</td><td><button className="text-button admin-action" type="button" disabled={changingId === investor.id} onClick={() => onChangeStatus(investor)}>{changingId === investor.id ? 'Salvando…' : investor.status === 'ACTIVE' ? 'Desativar' : 'Ativar'}</button></td></tr>)}</tbody></table>}</div><footer className="admin-pagination"><span>{data.totalElements} resultado(s) · página {data.page + 1} de {Math.max(1, data.totalPages)}</span><div><button className="secondary-button" type="button" disabled={data.page === 0} onClick={() => onPage(-1)}>Anterior</button><button className="secondary-button" type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(1)}>Próxima</button></div></footer></section>
}

function formatDate(value) { return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(value)) : '—' }

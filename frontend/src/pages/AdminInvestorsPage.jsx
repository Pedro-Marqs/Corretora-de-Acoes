import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeAdminInvestorStatus, listAdminInvestors } from '../api/admin-investors.js'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { EmptyState, ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'
import { Badge, Button, ConfirmDialog, DataTable, FilterBar, Pagination, SectionHeader, SelectField, StatusMessage, TextField } from '../components/ui/index.jsx'

export default function AdminInvestorsPage() {
  const auth = useAdminAuth()
  const { clear } = auth
  const navigate = useNavigate()
  const [filters, setFilters] = useState({ status: 'ALL', search: '', page: 0, size: 20, sort: 'createdAt,desc' })
  const [search, setSearch] = useState('')
  const [state, setState] = useState({ status: 'loading', data: null, message: '', loadedKey: '' })
  const [changingId, setChangingId] = useState(null)
  const [message, setMessage] = useState(null)
  const [confirmation, setConfirmation] = useState(null)

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

  function requestStatusChange(investor) {
    if (changingId) return
    const nextStatus = investor.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const action = nextStatus === 'ACTIVE' ? 'ativar' : 'desativar'
    setMessage(null)
    setConfirmation({ investor, nextStatus, action })
  }

  function closeConfirmation() {
    if (changingId) return
    setConfirmation(null)
  }

  async function confirmStatusChange() {
    if (!confirmation || changingId) return
    const { investor, nextStatus, action } = confirmation
    setChangingId(investor.id); setMessage(null)
    try {
      await changeAdminInvestorStatus(investor.id, nextStatus)
      setMessage({ kind: 'success', text: `Investidor ${action === 'ativar' ? 'ativado' : 'desativado'} com sucesso.` })
      setFilters((current) => ({ ...current }))
    } catch (error) {
      if (error.status === 401 || error.status === 403) { clear(); navigate('/admin/login', { replace: true }) }
      else if (error.status === 409) setMessage({ kind: 'conflict', text: 'A conta já está nesse estado ou a transição não é válida.' })
      else setMessage({ kind: 'error', text: error.message })
    } finally {
      setChangingId(null)
      setConfirmation(null)
    }
  }

  const loading = state.status === 'loading' || state.loadedKey !== JSON.stringify(filters)
  return <section className="admin-page"><SectionHeader eyebrow="Contas da corretora" title="Investidores" description="Dados pessoais protegidos e status operacional de cada conta." />
    {confirmation && <ConfirmDialog
      open
      title="Confirmar alteração de status"
      description={`Confirma ${confirmation.action} o investidor ${confirmation.investor.name}? A alteração será enviada à API administrativa.`}
      confirmLabel={`Confirmar ${confirmation.action}`}
      cancelLabel="Cancelar"
      tone={confirmation.nextStatus === 'INACTIVE' ? 'danger' : 'primary'}
      onClose={closeConfirmation}
      onConfirm={confirmStatusChange}
      pending={changingId === confirmation.investor.id}
    />}
    <FilterBar className="admin-investor-filters" aria-label="Filtros de investidores" onSubmit={submitSearch}>
      <TextField label="Buscar por nome ou e-mail" id="admin-search" value={search} onChange={(event) => setSearch(event.target.value)} maxLength="100" placeholder="Digite para buscar" />
      <SelectField label="Status" name="status" value={filters.status} onChange={updateFilter}><option value="ALL">Todos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></SelectField>
      <SelectField label="Ordenar" name="sort" value={filters.sort} onChange={updateFilter}><option value="createdAt,desc">Mais recentes</option><option value="createdAt,asc">Mais antigos</option><option value="name,asc">Nome A–Z</option><option value="name,desc">Nome Z–A</option></SelectField>
      <div className="ui-filter-bar__actions"><Button variant="secondary" type="submit">Buscar</Button></div>
    </FilterBar>
    {message && <StatusMessage className="admin-status-message" kind={message.kind} role={message.kind === 'error' ? 'alert' : 'status'} title={message.kind === 'success' ? 'Atualização concluída.' : message.kind === 'conflict' ? 'Conflito na atualização.' : 'Não foi possível atualizar.'}>{message.text}</StatusMessage>}
    {loading && <LoadingState message="Carregando investidores…" />}
    {!loading && state.status === 'error' && <ErrorState message={state.message} onRetry={() => setFilters((current) => ({ ...current }))} />}
    {!loading && state.status === 'ready' && <InvestorTable data={state.data} changingId={changingId} onChangeStatus={requestStatusChange} onPage={page} />}
  </section>
}

function InvestorTable({ data, changingId, onChangeStatus, onPage }) {
  const columns = [
    { key: 'name', label: 'Nome' },
    { key: 'emailMasked', label: 'E-mail' },
    { key: 'cpfMasked', label: 'CPF' },
    { key: 'status', label: 'Status', render: (investor) => <Badge tone={investor.status === 'ACTIVE' ? 'success' : 'danger'}>{investor.status === 'ACTIVE' ? 'Ativo' : 'Inativo'}</Badge> },
    { key: 'createdAt', label: 'Criado em', render: (investor) => formatDate(investor.createdAt) },
    { key: 'action', label: 'Ação', render: (investor) => <Button className="admin-action" variant="ghost" size="sm" type="button" disabled={changingId === investor.id} onClick={() => onChangeStatus(investor)}>{changingId === investor.id ? 'Salvando…' : investor.status === 'ACTIVE' ? 'Desativar' : 'Ativar'}</Button> },
  ]
  return <section className="admin-panel admin-investors-panel"><div className="admin-table-wrap">{data.content.length === 0 ? <EmptyState title="Nenhum investidor encontrado" description="Ajuste os filtros e tente novamente." /> : <DataTable caption="Investidores administrativos; dados pessoais mascarados." columns={columns} rows={data.content} rowKey={(investor) => investor.id} />}</div><Pagination page={data.page} totalPages={data.totalPages} onPageChange={(nextPage) => onPage(nextPage - data.page)} /></section>
}

function formatDate(value) { return value ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(value)) : '—' }

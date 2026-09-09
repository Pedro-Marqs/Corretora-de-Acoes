import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { getHistory } from '../api/history.js'
import { EmptyState, ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

const EMPTY_FILTERS = { from: '', to: '', type: '', ticker: '', brokerId: '', market: '' }
const LABELS = { INITIAL_BALANCE: 'Saldo inicial', DEPOSIT: 'Aporte', PURCHASE: 'Compra', SALE: 'Venda', TRANSFER: 'Transferência' }

LABELS.WITHDRAWAL = 'Retirada'

function brasiliaOffsetDateTime(value) {
  return value ? `${value.length === 16 ? value : value.slice(0, 16)}:00-03:00` : ''
}

function queryFilters(filters) {
  return {
    ...filters,
    ticker: filters.ticker.trim().toUpperCase(),
    from: brasiliaOffsetDateTime(filters.from),
    to: brasiliaOffsetDateTime(filters.to),
  }
}

export default function HistoryPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const sequence = useRef(0)
  const brokerSequence = useRef(0)
  const activeQueries = useRef(new Set())
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [applied, setApplied] = useState(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [filterErrors, setFilterErrors] = useState({})
  const [brokers, setBrokers] = useState([])
  const [brokerError, setBrokerError] = useState('')

  function expireSession() {
    sequence.current += 1
    setResult(null)
    auth?.clear?.()
    navigate('/login', { replace: true })
  }

  async function load(targetPage = page, targetFilters = applied) {
    const query = { page: targetPage, ...queryFilters(targetFilters) }
    const key = JSON.stringify(query)
    if (activeQueries.current.has(key)) return
    activeQueries.current.add(key)
    const request = ++sequence.current
    setStatus('loading')
    setError('')
    try {
      const data = await getHistory(query)
      if (request !== sequence.current) return
      setResult(data)
      setStatus('ready')
    } catch (cause) {
      if (request !== sequence.current) return
      if (cause?.status === 401) { expireSession(); return }
      setStatus('error')
      setError(cause?.message || 'Não foi possível consultar o histórico.')
    } finally {
      activeQueries.current.delete(key)
    }
  }

  async function loadBrokers() {
    const request = ++brokerSequence.current
    try {
      const items = await getActiveBrokers()
      if (request === brokerSequence.current) setBrokers(items)
    } catch (cause) {
      if (request !== brokerSequence.current) return
      if (cause?.status === 401) { expireSession(); return }
      setBrokerError(cause?.message || 'Não foi possível carregar as corretoras.')
    }
  }

  function retryBrokers() {
    setBrokerError('')
    loadBrokers()
  }

  useEffect(() => {
    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return
      loadBrokers()
      load(0, EMPTY_FILTERS)
    })
    return () => { cancelled = true; brokerSequence.current += 1; sequence.current += 1 }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function updateFilter(event) {
    const { name, value } = event.target
    setFilters((current) => ({ ...current, [name]: name === 'ticker' ? value.toUpperCase() : value }))
    setFilterErrors((current) => {
      if (name === 'from' || name === 'to') return { ...current, interval: '' }
      return { ...current, [name]: '' }
    })
  }

  function apply(event) {
    event.preventDefault()
    const nextErrors = {}
    if (filters.ticker && !/^[A-Z0-9.]{1,20}$/i.test(filters.ticker)) nextErrors.ticker = 'Use de 1 a 20 letras, números ou pontos no ticker.'
    if (filters.from && filters.to && filters.from > filters.to) nextErrors.interval = 'A data inicial deve ser anterior ou igual à data final.'
    setFilterErrors(nextErrors)
    if (Object.keys(nextErrors).length) {
      document.getElementById(nextErrors.interval ? 'history-from' : 'history-ticker')?.focus()
      return
    }
    setPage(0)
    setApplied(filters)
    load(0, filters)
  }

  function clearFilters() {
    setFilters(EMPTY_FILTERS)
    setApplied(EMPTY_FILTERS)
    setFilterErrors({})
    setPage(0)
    load(0, EMPTY_FILTERS)
  }

  function changePage(next) {
    if (status === 'loading' || next < 0 || next >= (result?.totalPages ?? 0)) return
    setPage(next)
    load(next, applied)
  }

  return <main className="history-page">
    <header className="history-heading"><p className="eyebrow">Movimentações</p><h1>Histórico</h1><p>Consulte operações concluídas e aportes registrados na sua conta.</p></header>
    <form className="history-filters" onSubmit={apply} noValidate>
      <div className="form-field"><label htmlFor="history-from">Data inicial</label><input id="history-from" name="from" type="datetime-local" value={filters.from} onChange={updateFilter} aria-invalid={Boolean(filterErrors.interval)} aria-describedby={filterErrors.interval ? 'history-interval-error' : undefined} /></div>
      <div className="form-field"><label htmlFor="history-to">Data final</label><input id="history-to" name="to" type="datetime-local" value={filters.to} onChange={updateFilter} aria-invalid={Boolean(filterErrors.interval)} aria-describedby={filterErrors.interval ? 'history-interval-error' : undefined} /></div>
      <div className="form-field"><label htmlFor="history-type">Tipo</label><select id="history-type" name="type" value={filters.type} onChange={updateFilter}><option value="">Todos</option>{Object.entries(LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
      <div className="form-field"><label htmlFor="history-ticker">Ticker</label><input id="history-ticker" name="ticker" maxLength="20" value={filters.ticker} onChange={updateFilter} aria-invalid={Boolean(filterErrors.ticker)} aria-describedby={filterErrors.ticker ? 'history-ticker-error' : undefined} placeholder="Ex.: PETR4" /></div>
      <div className="form-field"><label htmlFor="history-broker">Corretora</label><select id="history-broker" name="brokerId" value={filters.brokerId} onChange={updateFilter}><option value="">Todas</option>{brokers.map((broker) => <option key={broker.associationId} value={broker.associationId}>{broker.tradeName}</option>)}</select></div>
      <div className="form-field"><label htmlFor="history-market">Mercado</label><select id="history-market" name="market" value={filters.market} onChange={updateFilter}><option value="">Todos</option><option value="BR">Brasil</option><option value="US">Estados Unidos</option></select></div>
      {filterErrors.interval && <p id="history-interval-error" className="field-error history-filter-error" role="alert">{filterErrors.interval}</p>}
      {filterErrors.ticker && <p id="history-ticker-error" className="field-error history-filter-error" role="alert">{filterErrors.ticker}</p>}
      <div className="history-filter-actions"><button className="primary-button" type="submit" disabled={status === 'loading'}>Aplicar filtros</button><button className="secondary-button" type="button" onClick={clearFilters} disabled={status === 'loading'}>Limpar filtros</button></div>
    </form>
    {brokerError && <Message kind="error"><p>{brokerError}</p><button className="secondary-button" type="button" onClick={retryBrokers}>Tentar carregar corretoras novamente</button></Message>}
    <section className="history-results" aria-live="polite" aria-busy={status === 'loading'}>
      {status === 'loading' && <LoadingState message="Carregando histórico…" />}
      {status === 'error' && <ErrorState message={error} onRetry={() => load(page, applied)} />}
      {status === 'ready' && result?.content.length === 0 && <EmptyState title="Nenhuma movimentação encontrada" description="Ajuste os filtros ou volte mais tarde para consultar novos registros." />}
      {status === 'ready' && result?.content.length > 0 && <><div className="history-list">{result.content.map((movement) => <MovementCard key={movement.id} movement={movement} />)}</div><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={changePage} /></>}
    </section>
  </main>
}

function MovementCard({ movement }) {
  const trading = movement.type === 'PURCHASE' || movement.type === 'SALE'
  const transfer = movement.type === 'TRANSFER'
  const deposit = movement.type === 'DEPOSIT' || movement.type === 'INITIAL_BALANCE'
  const withdrawal = movement.type === 'WITHDRAWAL'
  return <article className="history-card">
    <header><span className={`history-type history-type-${movement.type.toLowerCase()}`}>{LABELS[movement.type]}</span><time dateTime={movement.occurredAt}>{formatBrasiliaDateTime(movement.occurredAt)}</time></header>
    <dl className="history-card-grid">
      {(trading || transfer) && <Detail label="Ativo" value={`${movement.ticker} · ${movement.market}`} />}
      {(trading || transfer) && <Detail label="Quantidade" value={`${movement.quantity} ações`} />}
      {trading && <Detail label="Corretora" value={movement.brokerName} />}
      {transfer && <Detail label="Origem" value={movement.originBrokerName} />}
      {transfer && <Detail label="Destino" value={movement.destinationBrokerName} />}
      {trading && <Detail label="Preço da cotação" value={formatMoney(movement.quotePrice, movement.currency)} />}
      {trading && <Detail label="Preço unitário em reais" value={formatCurrency(movement.unitPriceBrl)} />}
      {(trading || transfer || deposit || withdrawal) && <Detail label={deposit || withdrawal ? 'Valor' : 'Total'} value={formatCurrency(movement.totalAmount)} />}
      {movement.type === 'SALE' && <Detail label="Resultado realizado" value={formatCurrency(movement.realizedResult)} />}
      <Detail label="Saldo após movimento" value={formatCurrency(movement.remainingBalance)} />
    </dl>
  </article>
}

function Detail({ label, value }) { return <div><dt>{label}</dt><dd>{value || 'Não informado'}</dd></div> }

function Pagination({ page, totalPages, totalElements, onChange }) {
  if (totalPages <= 1) return <p className="history-count">{totalElements} {totalElements === 1 ? 'registro' : 'registros'}</p>
  return <nav className="history-pagination" aria-label="Paginação do histórico"><button className="secondary-button" type="button" onClick={() => onChange(page - 1)} disabled={page === 0}>Página anterior</button><span>Página {page + 1} de {totalPages} · {totalElements} registros</span><button className="secondary-button" type="button" onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>Próxima página</button></nav>
}

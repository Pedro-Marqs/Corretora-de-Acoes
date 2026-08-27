import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { associateBroker, getActiveBrokers, removeBroker, searchBroker } from '../api/brokers.js'
import { EmptyState, ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'

const onlyDigits = (value) => value.replace(/\D/g, '').slice(0, 14)
function formatCnpj(value) { return onlyDigits(value).replace(/^(\d{2})(\d)/, '$1.$2').replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3').replace(/\.(\d{3})(\d)/, '.$1/$2').replace(/(\/\d{4})(\d)/, '$1-$2') }
const address = (broker) => [broker.street, broker.complement, broker.district, `${broker.city} - ${broker.state}`, broker.postalCode].filter(Boolean).join(', ')

export default function BrokersPage() {
  const { clear } = useAuth(); const navigate = useNavigate()
  const [list, setList] = useState({ status: 'loading', items: [], message: '' })
  const [cnpj, setCnpj] = useState(''); const [cnpjError, setCnpjError] = useState(''); const [preview, setPreview] = useState(null)
  const [search, setSearch] = useState({ pending: false, error: '' }); const [association, setAssociation] = useState({ pending: false, message: '', error: '' })
  const [removal, setRemoval] = useState({ pendingId: null, errors: {} })
  const searchLock = useRef(false); const searchGeneration = useRef(0); const associationLock = useRef(false); const removalLocks = useRef(new Set())

  const expireSession = useCallback(() => { setList({ status: 'unauthenticated', items: [], message: '' }); setPreview(null); clear(); navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } }) }, [clear, navigate])
  const load = useCallback(async (preserve = false) => {
    if (!preserve) setList({ status: 'loading', items: [], message: '' })
    try { setList({ status: 'ready', items: await getActiveBrokers(), message: '' }); return true }
    catch (error) { if (error.status === 401) expireSession(); else if (preserve) setList((current) => ({ ...current, message: error.message })); else setList({ status: 'error', items: [], message: error.message }); return false }
  }, [expireSession])

  useEffect(() => { let active = true; getActiveBrokers().then((items) => { if (active) setList({ status: 'ready', items, message: '' }) }).catch((error) => { if (!active) return; if (error.status === 401) expireSession(); else setList({ status: 'error', items: [], message: error.message }) }); return () => { active = false } }, [expireSession])

  async function submitSearch(event) {
    event.preventDefault(); if (searchLock.current) return
    const normalized = onlyDigits(cnpj); if (normalized.length !== 14) { setCnpjError('Informe um CNPJ com 14 dígitos.'); return }
    const generation = searchGeneration.current
    searchLock.current = true; setCnpjError(''); setPreview(null); setSearch({ pending: true, error: '' }); setAssociation({ pending: false, message: '', error: '' })
    try { const result = await searchBroker(normalized); if (searchGeneration.current === generation) setPreview(result) } catch (error) { if (error.status === 401) expireSession(); else if (searchGeneration.current === generation) setSearch({ pending: false, error: error.message }) }
    finally { searchLock.current = false; setSearch((current) => ({ ...current, pending: false })) }
  }
  async function associate() {
    if (associationLock.current || !preview) return; associationLock.current = true; setAssociation({ pending: true, message: '', error: '' })
    try { await associateBroker(preview.cnpj); if (await load(true)) { setCnpj(''); setPreview(null); setAssociation({ pending: false, message: 'Corretora associada. A lista foi atualizada.', error: '' }) } }
    catch (error) { if (error.status === 401) expireSession(); else setAssociation({ pending: false, message: '', error: error.message }) }
    finally { associationLock.current = false; setAssociation((current) => ({ ...current, pending: false })) }
  }
  async function remove(associationId) {
    if (removalLocks.current.size > 0) return; removalLocks.current.add(associationId); setRemoval((current) => ({ pendingId: associationId, errors: { ...current.errors, [associationId]: undefined } }))
    try { await removeBroker(associationId); if (await load(true)) setAssociation({ pending: false, message: 'Corretora removida. A lista foi atualizada.', error: '' }) }
    catch (error) { if (error.status === 401) expireSession(); else setRemoval((current) => ({ pendingId: null, errors: { ...current.errors, [associationId]: error.message } })) }
    finally { removalLocks.current.delete(associationId); setRemoval((current) => ({ ...current, pendingId: current.pendingId === associationId ? null : current.pendingId })) }
  }

  if (list.status === 'loading') return <main className="brokers-page"><LoadingState message="Carregando corretoras…" /></main>
  if (list.status === 'error') return <main className="brokers-page"><ErrorState message={list.message} onRetry={() => load()} /></main>
  if (list.status !== 'ready') return null
  return <main className="brokers-page"><header className="brokers-heading"><p className="eyebrow">Instituições</p><h1>Minhas corretoras</h1><p>Pesquise uma instituição por CNPJ e escolha quais corretoras fazem parte da sua conta.</p></header>
    <section className="broker-search-card" aria-labelledby="broker-search-title"><div><p className="eyebrow">Nova associação</p><h2 id="broker-search-title">Pesquisar corretora</h2><p>A validação cadastral é feita pelo servidor antes da associação.</p></div><form onSubmit={submitSearch} noValidate><div className="form-field"><label htmlFor="brokerCnpj">CNPJ</label><input id="brokerCnpj" inputMode="numeric" placeholder="00.000.000/0000-00" value={cnpj} onChange={(event) => { searchGeneration.current += 1; setCnpj(formatCnpj(event.target.value)); setCnpjError(''); setPreview(null); setSearch({ pending: false, error: '' }); setAssociation({ pending: false, message: '', error: '' }) }} aria-invalid={Boolean(cnpjError)} aria-describedby={cnpjError ? 'brokerCnpj-error' : 'brokerCnpj-hint'} />{cnpjError ? <span id="brokerCnpj-error" className="field-error">{cnpjError}</span> : <span id="brokerCnpj-hint" className="field-hint">Digite os 14 dígitos do CNPJ.</span>}</div><button className="primary-button" type="submit" disabled={search.pending}>{search.pending ? 'Pesquisando…' : 'Pesquisar'}</button></form></section>
    {search.error && <Message kind="error">{search.error}</Message>}{preview && <Preview broker={preview} pending={association.pending} error={association.error} onAssociate={associate} />}{association.message && <Message kind="success">{association.message}</Message>}{list.message && <Message kind="error">{list.message}</Message>}
    <section className="brokers-list-section" aria-labelledby="brokers-list-title"><div className="brokers-list-heading"><div><p className="eyebrow">Associações ativas</p><h2 id="brokers-list-title">Corretoras da conta</h2></div><span>{list.items.length}</span></div>{list.items.length === 0 ? <EmptyState title="Nenhuma corretora associada" description="Pesquise um CNPJ para fazer a primeira associação." /> : <div className="broker-grid">{list.items.map((broker) => <Card key={broker.associationId} broker={broker} pending={removal.pendingId === broker.associationId} disabled={removal.pendingId !== null} error={removal.errors[broker.associationId]} onRemove={() => remove(broker.associationId)} />)}</div>}</section>
  </main>
}

function Details({ broker }) { return <dl><div><dt>Razão social</dt><dd>{broker.corporateName}</dd></div><div><dt>Nome fantasia</dt><dd>{broker.tradeName}</dd></div><div><dt>CNPJ</dt><dd>{formatCnpj(broker.cnpj)}</dd></div><div><dt>Situação cadastral</dt><dd>{broker.registrationStatus}</dd></div><div><dt>Categoria CVM</dt><dd>{broker.cvmCategory}</dd></div><div className="broker-address"><dt>Endereço</dt><dd>{address(broker)}</dd></div></dl> }
function Preview({ broker, pending, error, onAssociate }) { return <section className="broker-preview" aria-labelledby="broker-preview-title"><p className="eyebrow">Dados encontrados</p><h2 id="broker-preview-title">Confira antes de associar</h2><Details broker={broker} />{error && <Message kind="error">{error}</Message>}<button className="primary-button" type="button" onClick={onAssociate} disabled={pending}>{pending ? 'Associando…' : 'Associar corretora'}</button></section> }
function Card({ broker, pending, disabled, error, onRemove }) { return <article className="broker-card"><Details broker={broker} />{error && <Message kind="error">{error}</Message>}<button className="secondary-button" type="button" onClick={onRemove} disabled={disabled}>{pending ? 'Removendo…' : 'Remover corretora'}</button></article> }

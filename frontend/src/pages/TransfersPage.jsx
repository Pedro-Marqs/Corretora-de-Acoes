import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { getWalletPositions, transferPosition } from '../api/wallet.js'
import { useAuth } from '../context/auth-context.js'
import { formatCurrency } from '../utils/formatters.js'

export default function TransfersPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const loadLock = useRef(false)
  const sendLock = useRef(false)
  const confirmButton = useRef(null)
  const formButton = useRef(null)
  const [data, setData] = useState({ status: 'loading', snapshot: null, brokers: [], error: '' })
  const [form, setForm] = useState({ positionKey: '', destinationId: '', quantity: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [confirmation, setConfirmation] = useState(false)
  const [sending, setSending] = useState(false)
  const [message, setMessage] = useState({ kind: '', text: '' })

  function unauthorized() {
    setConfirmation(false)
    setData({ status: 'ready', snapshot: null, brokers: [], error: '' })
    auth?.clear?.()
    navigate('/login', { replace: true })
  }

  async function load({ preserve = false, success = '' } = {}) {
    if (loadLock.current) return false
    loadLock.current = true
    setData((current) => ({ ...current, status: preserve && current.snapshot ? 'ready' : 'loading', error: '' }))
    try {
      const [snapshot, brokers] = await Promise.all([getWalletPositions(), getActiveBrokers()])
      setData({ status: 'ready', snapshot, brokers, error: '' })
      if (success) setMessage({ kind: 'success', text: success })
      return true
    } catch (error) {
      if (error?.status === 401) { unauthorized(); return false }
      setData((current) => ({ ...current, status: preserve && current.snapshot ? 'ready' : 'error', error: error?.message || 'Não foi possível carregar os dados da transferência.' }))
      return false
    } finally { loadLock.current = false }
  }

  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const position = data.snapshot?.positions.find((item) => `${item.assetId}:${item.brokerageId}` === form.positionKey) ?? null
  const destinations = position ? data.brokers.filter((broker) => broker.associationId !== position.brokerageId) : []

  function update(name, value) {
    setForm((current) => ({ ...current, [name]: value, ...(name === 'positionKey' ? { destinationId: '', quantity: '' } : {}) }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setMessage({ kind: '', text: '' })
  }

  function validate() {
    const errors = {}
    const amount = Number(form.quantity)
    if (!position) errors.positionKey = 'Selecione uma posição aberta.'
    if (!form.destinationId) errors.destinationId = destinations.length ? 'Selecione uma corretora de destino.' : 'Associe outra corretora ativa para transferir.'
    else if (form.destinationId === position?.brokerageId) errors.destinationId = 'A corretora de destino deve ser diferente da origem.'
    if (!form.quantity || !Number.isInteger(amount) || amount <= 0) errors.quantity = 'Informe uma quantidade inteira positiva.'
    else if (position && amount > Number(position.quantity)) errors.quantity = `A quantidade máxima disponível é ${position.quantity}.`
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  function requestConfirmation(event) {
    event.preventDefault()
    if (sending || !validate()) return
    formButton.current = event.nativeEvent.submitter
    setConfirmation(true)
  }

  function closeConfirmation() {
    if (sending) return
    setConfirmation(false)
    queueMicrotask(() => formButton.current?.focus())
  }

  async function confirm() {
    if (sendLock.current || !position || !validate()) return
    sendLock.current = true
    setSending(true)
    setMessage({ kind: '', text: '' })
    try {
      await transferPosition(position.brokerageId, form.destinationId, position.assetId, Number(form.quantity))
      setConfirmation(false)
      const refreshed = await load({ preserve: true, success: 'Transferência concluída. As posições foram atualizadas sem alterar o saldo.' })
      if (refreshed) setForm({ positionKey: '', destinationId: '', quantity: '' })
    } catch (error) {
      if (error?.status === 401) { unauthorized(); return }
      setConfirmation(false)
      setMessage({ kind: 'error', text: error?.message || 'Não foi possível concluir a transferência.' })
      queueMicrotask(() => formButton.current?.focus())
    } finally { sendLock.current = false; setSending(false) }
  }

  if (data.status === 'loading') return <main className="transfers-page"><div className="common-state" role="status"><span className="state-spinner" aria-hidden="true" /><p>Carregando posições e corretoras…</p></div></main>
  if (data.status === 'error') return <main className="transfers-page"><div className="common-state" role="alert"><p>{data.error}</p><button className="secondary-button" type="button" onClick={() => load()}>Tentar novamente</button></div></main>

  return <main className="transfers-page">
    <header className="transfers-heading"><p className="eyebrow">Carteira</p><h1>Transferir posição</h1><p>Transfira ações entre suas corretoras ativas. O saldo da conta não é alterado.</p></header>
    {message.text && <div className={message.kind === 'success' ? 'message operation-success transfer-message' : 'error-banner transfer-message'} role={message.kind === 'success' ? 'status' : 'alert'}>{message.text}</div>}
    {data.error && <div className="error-banner transfer-message" role="alert">{data.error}<button className="text-button" type="button" onClick={() => load({ preserve: true })}>Tentar novamente</button></div>}
    {!data.snapshot?.positions.length ? <section className="transfer-card"><div className="empty-state"><h2>Nenhuma posição aberta</h2><p>Quando houver ativos na carteira, eles aparecerão aqui para transferência.</p></div></section> : <>
      <form className="transfer-card" onSubmit={requestConfirmation} noValidate>
        <Field label="Posição aberta" error={fieldErrors.positionKey} errorId="transfer-position-error"><select id="transfer-position" value={form.positionKey} onChange={(event) => update('positionKey', event.target.value)} aria-invalid={Boolean(fieldErrors.positionKey)} aria-describedby={fieldErrors.positionKey ? 'transfer-position-error' : undefined}><option value="">Selecione</option>{data.snapshot.positions.map((item) => <option key={`${item.assetId}:${item.brokerageId}`} value={`${item.assetId}:${item.brokerageId}`}>{item.ticker} · {item.brokerageName} · {item.quantity} ações</option>)}</select></Field>
        <div className="form-field"><label htmlFor="transfer-origin">Corretora de origem</label><input id="transfer-origin" value={position?.brokerageName ?? ''} placeholder="Derivada da posição" readOnly /></div>
        <Field label="Corretora de destino" error={fieldErrors.destinationId} errorId="transfer-destination-error"><select id="transfer-destination" value={form.destinationId} onChange={(event) => update('destinationId', event.target.value)} disabled={!position} aria-invalid={Boolean(fieldErrors.destinationId)} aria-describedby={fieldErrors.destinationId ? 'transfer-destination-error' : undefined}><option value="">Selecione</option>{destinations.map((broker) => <option key={broker.associationId} value={broker.associationId}>{broker.tradeName}</option>)}</select></Field>
        <div className="form-field"><label htmlFor="transfer-quantity">Quantidade</label><input id="transfer-quantity" inputMode="numeric" value={form.quantity} onChange={(event) => update('quantity', event.target.value)} disabled={!position} aria-invalid={Boolean(fieldErrors.quantity)} aria-describedby={fieldErrors.quantity ? 'transfer-quantity-error' : 'transfer-quantity-hint'} />{fieldErrors.quantity ? <span id="transfer-quantity-error" className="field-error">{fieldErrors.quantity}</span> : <span id="transfer-quantity-hint" className="field-hint">{position ? `${position.quantity} ações disponíveis.` : 'Selecione uma posição.'}</span>}</div>
        <button className="primary-button" type="submit" disabled={sending}>Continuar</button>
      </form>
      <section className="transfer-positions" aria-labelledby="transfer-list-title"><header><div><p className="eyebrow">Fonte oficial</p><h2 id="transfer-list-title">Posições abertas</h2></div><p>Saldo disponível <strong>{formatCurrency(data.snapshot.availableBalance)}</strong></p></header><ul>{data.snapshot.positions.map((item) => <li key={`${item.assetId}:${item.brokerageId}`}><span><strong>{item.ticker}</strong><small>{item.name} · {item.market}</small></span><span><strong>{item.quantity} ações</strong><small>{item.brokerageName}</small></span><span><small>Preço médio</small><strong>{formatCurrency(item.averagePriceBrl)}</strong></span></li>)}</ul></section>
    </>}
    {confirmation && position && <TransferConfirmation position={position} destination={data.brokers.find((item) => item.associationId === form.destinationId)} quantity={form.quantity} pending={sending} confirmRef={confirmButton} onCancel={closeConfirmation} onConfirm={confirm} />}
  </main>
}

function Field({ label, error, errorId, children }) {
  return <div className="form-field"><label htmlFor={children.props.id}>{label}</label>{children}{error && <span id={errorId} className="field-error">{error}</span>}</div>
}

function TransferConfirmation({ position, destination, quantity, pending, confirmRef, onCancel, onConfirm }) {
  const cancel = useRef(null)
  useEffect(() => { confirmRef.current?.focus() }, [confirmRef])
  function keyDown(event) {
    if (event.key === 'Escape' && !pending) { event.preventDefault(); onCancel(); return }
    if (event.key !== 'Tab') return
    if (!event.shiftKey && document.activeElement === confirmRef.current) { event.preventDefault(); cancel.current?.focus() }
    if (event.shiftKey && document.activeElement === cancel.current) { event.preventDefault(); confirmRef.current?.focus() }
  }
  return <div className="transfer-confirmation"><section role="dialog" aria-modal="true" aria-labelledby="transfer-confirmation-title" onKeyDown={keyDown}><h2 id="transfer-confirmation-title">Confirmar transferência</h2><p>Transferir <strong>{quantity} ações de {position.ticker}</strong>, de <strong>{position.brokerageName}</strong> para <strong>{destination?.tradeName}</strong>?</p><div className="confirmation-actions"><button ref={cancel} className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button><button ref={confirmRef} className="primary-button" type="button" onClick={onConfirm} disabled={pending}>{pending ? 'Transferindo…' : 'Confirmar transferência'}</button></div></section></div>
}

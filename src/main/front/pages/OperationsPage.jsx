import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { searchAsset } from '../api/market.js'
import { getWalletPositions, purchaseAsset, sellAsset } from '../api/wallet.js'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

const initialSearch = { ticker: '', market: 'BR' }

export default function OperationsPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState(initialSearch)
  const [snapshot, setSnapshot] = useState(null)
  const [brokers, setBrokers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const openerRef = useRef(null)

  async function load() {
    setLoading(true); setError('')
    try {
      const [positions, activeBrokers] = await Promise.all([getWalletPositions(), getActiveBrokers()])
      setSnapshot(positions); setBrokers(activeBrokers)
    } catch (failure) {
      if (failure?.status === 401) { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }); return }
      setSnapshot(null); setError(failure?.message || 'Não foi possível carregar sua carteira.')
    } finally { setLoading(false) }
  }

  useEffect(() => {
    async function loadInitialData() {
      await Promise.resolve()
      await load()
    }

    loadInitialData()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function submitSearch(event) {
    event.preventDefault(); setError('')
    if (!search.ticker.trim()) { setError('Informe um ticker.'); return }
    try {
      const asset = await searchAsset(search.ticker, search.market)
      if (!asset) { setError('Nenhum ativo foi encontrado.'); return }
      openerRef.current = event.nativeEvent.submitter
      setSelected({ asset, position: null })
    } catch (failure) {
      if (failure?.status === 401) { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }); return }
      setError(failure?.message || 'Não foi possível pesquisar o ativo.')
    }
  }

  function selectPosition(position, event) {
    openerRef.current = event.currentTarget
    setSelected({ asset: position, position })
  }

  return <main className="private-page operations-page">
    <header className="operations-heading"><p className="eyebrow">Carteira</p><h1>Compra e venda</h1></header>
    <form className="operations-search" onSubmit={submitSearch} noValidate>
      <div className="form-field"><label htmlFor="operation-ticker">Ticker</label><input id="operation-ticker" value={search.ticker} onChange={(event) => setSearch((value) => ({ ...value, ticker: event.target.value.toUpperCase() }))} placeholder="PETR4" /></div>
      <div className="form-field"><label htmlFor="operation-market">Bolsa / mercado</label><select id="operation-market" value={search.market} onChange={(event) => setSearch((value) => ({ ...value, market: event.target.value }))}><option value="BR">Brasil (B3)</option><option value="US">Estados Unidos</option></select></div>
      <button className="primary-button" type="submit">Buscar ativo</button>
    </form>
    {error && <div className="error-banner operation-message" role="alert">{error}<button className="text-button" type="button" onClick={load}>Tentar novamente</button></div>}
    <section className="positions-section" aria-labelledby="positions-title">
      <header><div><p className="eyebrow">Seus investimentos</p><h2 id="positions-title">Posições abertas</h2></div>{snapshot && <p>Saldo disponível <strong>{formatCurrency(snapshot.availableBalance)}</strong></p>}</header>
      {loading && <p role="status">Carregando posições…</p>}
      {!loading && !error && snapshot?.positions.length === 0 && <div className="empty-state"><h3>Sua carteira ainda está vazia</h3><p>Pesquise um ticker acima para iniciar uma operação.</p></div>}
      {!loading && snapshot?.positions.length > 0 && <ul className="positions-list">{snapshot.positions.map((position) => <li key={`${position.assetId}-${position.brokerageId}`}><button type="button" onClick={(event) => selectPosition(position, event)}><span><strong>{position.ticker}</strong><small>{position.name} · {position.market} · {position.brokerageName}</small></span><span><strong>{position.quantity} ações</strong><small>Preço médio {formatCurrency(position.averagePriceBrl)}</small></span><span className={Number(position.unrealizedResultBrl) >= 0 ? 'positive-value' : 'negative-value'}>{position.unrealizedResultBrl == null ? 'Resultado indisponível' : formatCurrency(position.unrealizedResultBrl)}</span></button></li>)}</ul>}
    </section>
    {selected && <OperationModal context={selected} brokers={brokers} positions={snapshot?.positions ?? []} availableBalance={snapshot?.availableBalance} openerRef={openerRef} onClose={() => setSelected(null)} onUnauthorized={() => { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }) }} onCompleted={async () => { setSelected(null); await load() }} />}
  </main>
}

function OperationModal({ context, brokers, positions, availableBalance, openerRef, onClose, onUnauthorized, onCompleted }) {
  const dialogRef = useRef(null)
  const quantityRef = useRef(null)
  const [quantity, setQuantity] = useState('')
  const [brokerageId, setBrokerageId] = useState(context.position?.brokerageId ?? brokers[0]?.associationId ?? '')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const asset = context.asset
  const priceBrl = asset.quotePriceBrl ?? asset.priceBrl ?? null
  const estimate = priceBrl != null && Number.isInteger(Number(quantity)) && Number(quantity) > 0 ? Number(priceBrl) * Number(quantity) : null

  function close() { onClose(); queueMicrotask(() => openerRef.current?.focus()) }
  useEffect(() => { quantityRef.current?.focus() }, [])
  function keyDown(event) {
    if (event.key === 'Escape' && !sending) { event.preventDefault(); close(); return }
    if (event.key !== 'Tab') return
    const items = [...dialogRef.current.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled)')]
    const first = items[0], last = items.at(-1)
    if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  }
  async function operate(kind) {
    const amount = Number(quantity)
    if (!Number.isInteger(amount) || amount <= 0) { setError('Informe uma quantidade inteira positiva.'); quantityRef.current?.focus(); return }
    if (!brokerageId) { setError('Selecione uma corretora ativa.'); return }
    if (sending) return
    setSending(true); setError('')
    try {
      await (kind === 'sale' ? sellAsset : purchaseAsset)(asset.assetId, brokerageId, amount)
      await onCompleted()
    } catch (failure) {
      if (failure?.status === 401) { onUnauthorized(); return }
      setError(failure?.message || 'Não foi possível concluir a operação.')
    } finally { setSending(false) }
  }

  const position = positions.find((item) => item.assetId === asset.assetId && item.brokerageId === brokerageId) ?? null
  return <div className="operation-modal" role="presentation"><section ref={dialogRef} className="operation-modal-card" role="dialog" aria-modal="true" aria-labelledby="operation-title" onKeyDown={keyDown}>
    <header><div><p className="eyebrow">{asset.market} · {asset.currency}</p><h2 id="operation-title">{asset.ticker} <small>{asset.name}</small></h2></div><button type="button" className="modal-close" aria-label="Fechar operação" onClick={close} disabled={sending}>×</button></header>
    {error && <div className="error-banner" role="alert">{error}</div>}
    <div className="operation-price"><span>Preço fixo do backend</span><strong>{priceBrl == null ? 'Indisponível' : formatCurrency(priceBrl)}</strong>{asset.quotePrice != null && asset.currency === 'USD' && <small>{formatMoney(asset.quotePrice, 'USD')} na moeda original</small>}{asset.quoteQuotedAt && <small>Cotação de {formatBrasiliaDateTime(asset.quoteQuotedAt)}</small>}</div>
    {(asset.quoteStale || asset.exchangeRateStale) && <div className="warning-banner">{asset.quoteStale && <p>Cotação desatualizada</p>}{asset.exchangeRateStale && <p>USD/BRL desatualizado</p>}</div>}
    <div className="operation-modal-grid"><div className="form-field"><label htmlFor="operation-quantity">Quantidade</label><input ref={quantityRef} id="operation-quantity" inputMode="numeric" value={quantity} onChange={(event) => setQuantity(event.target.value.replace(/\D/g, ''))} aria-describedby={error ? 'operation-error' : undefined} /></div><div className="form-field"><label htmlFor="operation-broker">Corretora</label><select id="operation-broker" value={brokerageId} onChange={(event) => setBrokerageId(event.target.value)}><option value="">Selecione</option>{brokers.map((broker) => <option key={broker.associationId} value={broker.associationId}>{broker.tradeName}</option>)}</select></div></div>
    <dl className="operation-snapshot operation-balance"><div><dt>Saldo disponível</dt><dd>{availableBalance == null ? 'Indisponível' : formatCurrency(availableBalance)}</dd><small>Limite informativo para compra</small></div><div><dt>Valor estimado</dt><dd>{estimate == null ? 'Informe a quantidade' : formatCurrency(estimate)}</dd></div></dl>
    {position && <dl className="operation-snapshot operation-position" aria-label="Posição atual"><div><dt>Corretora da posição</dt><dd>{position.brokerageName}</dd></div><div><dt>Quantidade da posição</dt><dd>{position.quantity}</dd></div><div><dt>Preço médio acumulado</dt><dd>{position.averagePriceBrl == null ? 'Indisponível' : formatCurrency(position.averagePriceBrl)}</dd></div><div><dt>Lucro / perda</dt><dd>{position.unrealizedResultBrl == null ? 'Indisponível' : formatCurrency(position.unrealizedResultBrl)}</dd></div></dl>}
    <p id="operation-error" className="field-error" aria-live="polite">{error}</p>
    <div className="modal-actions"><button type="button" className="secondary-button negative-action" onClick={() => operate('sale')} disabled={sending || priceBrl == null}>Vender</button><button type="button" className="primary-button" onClick={() => operate('purchase')} disabled={sending || priceBrl == null}>{sending ? 'Enviando…' : 'Comprar'}</button></div>
  </section></div>
}

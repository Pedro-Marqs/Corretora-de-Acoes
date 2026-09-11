import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getActiveBrokers } from '../api/brokers.js'
import { searchAsset } from '../api/market.js'
import { getWalletPositions, purchaseAsset, sellAsset } from '../api/wallet.js'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

const initialSearch = { ticker: '', market: 'BR' }

export default function OperationsPage() {
  const auth = useAuth(); const navigate = useNavigate(); const location = useLocation()
  const [search, setSearch] = useState(initialSearch); const [snapshot, setSnapshot] = useState(null)
  const [brokers, setBrokers] = useState([]); const [loading, setLoading] = useState(true)
  const [error, setError] = useState(''); const [selected, setSelected] = useState(null)
  const openerRef = useRef(null)

  async function load() {
    setLoading(true); setError('')
    try {
      const [positions, activeBrokers] = await Promise.all([getWalletPositions(), getActiveBrokers()])
      setSnapshot(positions); setBrokers(activeBrokers)
      if (location.state?.asset) { setSelected({ asset: location.state.asset, position: null }); navigate(location.pathname, { replace: true, state: null }) }
    } catch (failure) {
      if (failure?.status === 401) { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }); return }
      setSnapshot(null); setError(failure?.message || 'Não foi possível carregar sua carteira.')
    } finally { setLoading(false) }
  }
  useEffect(() => { async function loadInitialData() { await Promise.resolve(); await load() }; loadInitialData() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function submitSearch(event) {
    event.preventDefault(); setError('')
    if (!search.ticker.trim()) { setError('Informe um ticker.'); return }
    try {
      const asset = await searchAsset(search.ticker, search.market)
      if (!asset) { setError('Nenhum ativo foi encontrado.'); return }
      openerRef.current = event.nativeEvent.submitter; setSelected({ asset, position: null })
    } catch (failure) {
      if (failure?.status === 401) { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }); return }
      setError(failure?.message || 'Não foi possível pesquisar o ativo.')
    }
  }
  function selectPosition(position, event) { openerRef.current = event.currentTarget; setSelected({ asset: position, position }) }

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
      {!loading && !error && <div className="positions-table" role="table" aria-label="Posições abertas"><div className="positions-table-head" role="row"><span role="columnheader">Ticker</span><span role="columnheader">Quantidade</span><span role="columnheader">Saldo / posição</span><span role="columnheader">Preço atual</span><span role="columnheader">Preço médio</span><span role="columnheader">Rentabilidade</span></div>{snapshot?.positions.length === 0 && <div className="empty-state"><h3>Sua carteira ainda está vazia</h3><p>Pesquise um ticker acima para iniciar uma operação.</p></div>}{snapshot?.positions.length > 0 && <ul className="positions-list">{snapshot.positions.map((position) => <li key={position.assetId + '-' + position.brokerageId}><button className="position-row" type="button" onClick={(event) => selectPosition(position, event)}><span className="position-cell position-ticker"><strong>{position.ticker}</strong><small>{position.name} · {position.market} · {position.brokerageName}</small></span><span className="position-cell"><strong>{position.quantity}</strong></span><span className="position-cell"><strong>{position.marketValueBrl == null ? 'Indisponível' : formatCurrency(position.marketValueBrl)}</strong><small>Valor da posição</small></span><span className="position-cell"><strong>{currentPrice(position)}</strong>{position.market === 'US' && <small>({formatCurrency(position.quotePriceBrl)})</small>}</span><span className="position-cell"><strong>{averagePrice(position)}</strong>{position.market === 'US' && <small>({formatCurrency(position.averagePriceBrl)})</small>}</span><span className={'position-cell ' + (Number(position.unrealizedResultBrl) >= 0 ? 'positive-value' : 'negative-value')}><strong>{position.unrealizedResultBrl == null ? 'Indisponível' : formatCurrency(position.unrealizedResultBrl)}</strong><small>{profitability(position)}</small></span></button></li>)}</ul>}</div>}
    </section>
    {selected && <OperationModal context={selected} brokers={brokers} positions={snapshot?.positions ?? []} availableBalance={snapshot?.availableBalance} openerRef={openerRef} onClose={() => setSelected(null)} onUnauthorized={() => { setSnapshot(null); auth?.clear?.(); navigate('/login', { replace: true }) }} onCompleted={async () => { setSelected(null); await load() }} />}
  </main>
}

function OperationModal({ context, brokers, positions, availableBalance, openerRef, onClose, onUnauthorized, onCompleted }) {
  const dialogRef = useRef(null); const quantityRef = useRef(null)
  const [quantity, setQuantity] = useState(''); const [brokerageId, setBrokerageId] = useState(context.position?.brokerageId ?? brokers[0]?.associationId ?? '')
  const [error, setError] = useState(''); const [sending, setSending] = useState(false)
  const [unitPrice, setUnitPrice] = useState(() => initialNativePrice(context.asset)); const [occurredAt, setOccurredAt] = useState(defaultOperationDate); const [confirmation, setConfirmation] = useState(null)
  const asset = context.asset; const priceBrl = asset.quotePriceBrl ?? asset.priceBrl ?? null
  function close() { onClose(); queueMicrotask(() => openerRef.current?.focus()) }
  useEffect(() => { quantityRef.current?.focus() }, [])
  function keyDown(event) {
    if (event.key === 'Escape' && !sending) { event.preventDefault(); close(); return }
    if (event.key !== 'Tab') return
    const items = [...dialogRef.current.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled)')]; const first = items[0]; const last = items.at(-1)
    if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  }
  function requestOperation(kind) {
    const amount = Number(quantity)
    if (!Number.isInteger(amount) || amount <= 0) { setError('Informe uma quantidade inteira positiva.'); quantityRef.current?.focus(); return }
    if (!brokerageId) { setError('Selecione uma corretora ativa.'); return }
    if (!Number.isFinite(Number(unitPrice)) || Number(unitPrice) <= 0) { setError('Informe um preço unitário positivo.'); return }
    const instant = operationInstant(occurredAt)
    if (!instant || Number.isNaN(Date.parse(instant))) { setError('Informe uma data e hora válidas.'); return }
    if (Date.parse(instant) > Date.now()) { setError('A data e hora não podem estar no futuro.'); return }
    setError(''); setConfirmation(kind)
  }
  async function operate(kind) {
    if (sending) return
    setSending(true); setError('')
    try {
      await (kind === 'sale' ? sellAsset : purchaseAsset)(asset.assetId, brokerageId, Number(quantity), unitPrice, operationInstant(occurredAt))
      await onCompleted()
    } catch (failure) {
      if (failure?.status === 401) { onUnauthorized(); return }
      setError(failure?.message || 'Não foi possível concluir a operação.')
    } finally { setSending(false); setConfirmation(null) }
  }
  const position = positions.find((item) => item.assetId === asset.assetId && item.brokerageId === brokerageId) ?? null
  return <div className="operation-modal" role="presentation"><section ref={dialogRef} className="operation-modal-card" role="dialog" aria-modal="true" aria-labelledby="operation-title" onKeyDown={keyDown}>
    <header><div><p className="eyebrow">{asset.market} · {asset.currency}</p><h2 id="operation-title">{asset.ticker} <small>{asset.name}</small></h2></div><button type="button" className="modal-close" aria-label="Fechar operação" onClick={close} disabled={sending}>×</button></header>
    {error && <div className="error-banner" role="alert">{error}</div>}
    <div className="operation-price"><span>{asset.quoteStale ? 'Preço de fechamento oficial' : 'Preço oficial de referência'}</span><strong>{priceBrl == null ? 'Indisponível' : formatCurrency(priceBrl)}</strong>{asset.quotePrice != null && asset.currency === 'USD' && <small>{formatMoney(asset.quotePrice, 'USD')} na moeda original</small>}{asset.quoteQuotedAt && <small>{asset.quoteStale ? 'Observado em' : 'Cotação de'} {formatBrasiliaDateTime(asset.quoteQuotedAt)}</small>}</div>
    {(asset.quoteStale || asset.exchangeRateStale) && <div className="warning-banner">{asset.quoteStale && <p>Preço de fechamento: cotação do instante informado acima.</p>}{asset.exchangeRateStale && <p>USD/BRL desatualizado.</p>}</div>}
    <div className="operation-modal-grid"><div className="form-field"><label htmlFor="operation-quantity">Quantidade</label><input ref={quantityRef} id="operation-quantity" inputMode="numeric" value={quantity} onChange={(event) => setQuantity(event.target.value.replace(/\D/g, ''))} aria-describedby={error ? 'operation-error' : undefined} /></div><div className="form-field"><label htmlFor="operation-broker">Corretora</label><select id="operation-broker" value={brokerageId} onChange={(event) => setBrokerageId(event.target.value)}><option value="">Selecione</option>{brokers.map((broker) => <option key={broker.associationId} value={broker.associationId}>{broker.tradeName}</option>)}</select></div><div className="form-field"><label htmlFor="operation-price-input">Preço unitário ({asset.currency})</label><input id="operation-price-input" type="number" min="0.01" step="0.01" inputMode="decimal" value={unitPrice} onChange={(event) => setUnitPrice(event.target.value)} aria-describedby={error ? 'operation-error' : undefined} /></div><div className="form-field"><label htmlFor="operation-occurred-at">Data e hora da operação</label><input id="operation-occurred-at" type="datetime-local" value={occurredAt} onChange={(event) => setOccurredAt(event.target.value)} aria-describedby={error ? 'operation-error' : undefined} /></div></div>
    <dl className="operation-snapshot operation-balance"><div><dt>Saldo disponível</dt><dd>{availableBalance == null ? 'Indisponível' : formatCurrency(availableBalance)}</dd><small>Limite informativo para compra</small></div><div><dt>Preço oficial unitário</dt><dd>{priceBrl == null ? 'Indisponível' : formatCurrency(priceBrl)}</dd><small>O total será calculado pelo servidor</small></div></dl>
    {position && <dl className="operation-snapshot operation-position" aria-label="Posição atual"><div><dt>Corretora da posição</dt><dd>{position.brokerageName}</dd></div><div><dt>Quantidade da posição</dt><dd>{position.quantity}</dd></div><div><dt>Preço médio acumulado</dt><dd>{position.averagePriceBrl == null ? 'Indisponível' : formatCurrency(position.averagePriceBrl)}</dd></div><div><dt>Lucro / perda</dt><dd>{position.unrealizedResultBrl == null ? 'Indisponível' : formatCurrency(position.unrealizedResultBrl)}</dd></div></dl>}
    <p id="operation-error" className="field-error" aria-live="polite">{error}</p>
    <div className="modal-actions"><button type="button" className="secondary-button negative-action" onClick={() => requestOperation('sale')} disabled={sending || priceBrl == null}>Vender</button><button type="button" className="primary-button" onClick={() => requestOperation('purchase')} disabled={sending || priceBrl == null}>{sending ? 'Enviando…' : 'Comprar'}</button></div>
    {confirmation && <section className="operation-confirmation" role="dialog" aria-modal="true" aria-labelledby="operation-confirmation-title"><h3 id="operation-confirmation-title">Confirmar {confirmation === 'sale' ? 'venda' : 'compra'}</h3><p>{confirmation === 'sale' ? 'Vender' : 'Comprar'} {quantity} unidade(s) de {asset.ticker} por {formatMoney(unitPrice, asset.currency)} cada, em {occurredAt.replace('T', ' ')}?</p><div className="modal-actions"><button className="secondary-button" type="button" onClick={() => setConfirmation(null)} disabled={sending}>Cancelar</button><button className="primary-button" type="button" onClick={() => operate(confirmation)} disabled={sending}>{sending ? 'Enviando…' : 'Confirmar operação'}</button></div></section>}
  </section></div>
}

function initialNativePrice(asset) { return String(asset.originalPrice ?? asset.quotePrice ?? (asset.currency === 'BRL' ? asset.priceBrl ?? '' : '')) }
function defaultOperationDate() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date()).replace(' ', 'T')
}
function operationInstant(value) { return value && value + ':00-03:00' }
function currentPrice(position) { return position.quotePrice == null ? 'Indisponível' : position.market === 'US' ? formatMoney(position.quotePrice, 'USD') : formatCurrency(position.quotePriceBrl) }
function averagePrice(position) { if (position.averagePriceBrl == null) return 'Indisponível'; if (position.market !== 'US') return formatCurrency(position.averagePriceBrl); const rate = Number(position.usdBrlRate); return rate > 0 ? formatMoney(Number(position.averagePriceBrl) / rate, 'USD') : 'Indisponível' }
function profitability(position) { const result = Number(position.unrealizedResultBrl); const invested = Number(position.averagePriceBrl) * Number(position.quantity); return Number.isFinite(result) && invested > 0 ? `${(result / invested * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%` : 'Percentual indisponível' }

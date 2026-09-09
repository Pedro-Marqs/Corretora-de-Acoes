import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchAsset } from '../api/market.js'
import { EmptyState, ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatMoney } from '../utils/formatters.js'

const POPULAR_ASSETS = [
  { ticker: 'PETR4', market: 'BR', label: 'Petrobras' },
  { ticker: 'VALE3', market: 'BR', label: 'Vale' },
  { ticker: 'ITUB4', market: 'BR', label: 'Itaú Unibanco' },
  { ticker: 'BBAS3', market: 'BR', label: 'Banco do Brasil' },
  { ticker: 'AAPL', market: 'US', label: 'Apple' },
  { ticker: 'NVDA', market: 'US', label: 'NVIDIA' },
]

export default function AssetsPage() {
  const { clear: clearAuth } = useAuth(); const navigate = useNavigate(); const lock = useRef(false)
  const [ticker, setTicker] = useState(''); const [market, setMarket] = useState(''); const [searchedTicker, setSearchedTicker] = useState('')
  const [fieldErrors, setFieldErrors] = useState({ ticker: '', market: '' })
  const [state, setState] = useState({ status: 'initial', asset: null, message: '' })

  async function searchValues(rawTicker, rawMarket) {
    if (lock.current) return
    const normalized = rawTicker.trim().toUpperCase(); const normalizedMarket = rawMarket.trim().toUpperCase()
    const errors = {
      ticker: /^[A-Z0-9]{1,12}$/.test(normalized) ? '' : 'Informe um ticker com 1 a 12 letras ou números.',
      market: ['BR', 'US'].includes(normalizedMarket) ? '' : 'Selecione o mercado do ativo.',
    }
    if (errors.ticker || errors.market) { setFieldErrors(errors); return }
    lock.current = true; setTicker(normalized); setMarket(normalizedMarket); setSearchedTicker(normalized); setFieldErrors({ ticker: '', market: '' }); setState({ status: 'loading', asset: null, message: '' })
    try {
      const asset = await searchAsset(normalized, normalizedMarket); setState({ status: asset ? 'success' : 'empty', asset, message: '' })
    } catch (error) {
      if (error.status === 401) { clearAuth(); navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } }) }
      else setState({ status: 'error', asset: null, message: error.message })
    } finally { lock.current = false }
  }

  function submit(event) { event?.preventDefault(); return searchValues(ticker, market) }
  function selectPopular(asset) { setTicker(asset.ticker); setMarket(asset.market); return searchValues(asset.ticker, asset.market) }

  return <main className="assets-page"><header className="assets-heading"><p className="eyebrow">Dados de mercado</p><h1>Pesquisa de ativos</h1><p>Consulte cotações oficiais da simulação pelo ticker.</p></header>
    <section className="asset-search-card" aria-labelledby="asset-search-title"><div className="asset-search-intro"><h2 id="asset-search-title">Encontre um ativo</h2><p>Informe o ticker e escolha onde o ativo é negociado.</p></div><form className="asset-search-form" onSubmit={submit} noValidate><div className="form-field"><label htmlFor="assetTicker">Ticker <span aria-hidden="true">*</span></label><input id="assetTicker" name="ticker" autoComplete="off" maxLength="12" required value={ticker} onChange={(event) => { setTicker(event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '')); setFieldErrors((current) => ({ ...current, ticker: '' })) }} aria-invalid={Boolean(fieldErrors.ticker)} aria-describedby={fieldErrors.ticker ? 'assetTicker-error' : 'assetTicker-hint'} placeholder="PETR4" />{fieldErrors.ticker ? <span id="assetTicker-error" className="field-error">{fieldErrors.ticker}</span> : <span id="assetTicker-hint" className="field-hint">Use de 1 a 12 letras ou números.</span>}</div><div className="form-field"><label htmlFor="assetMarket">Mercado <span aria-hidden="true">*</span></label><select id="assetMarket" name="market" required value={market} onChange={(event) => { setMarket(event.target.value); setFieldErrors((current) => ({ ...current, market: '' })) }} aria-invalid={Boolean(fieldErrors.market)} aria-describedby={fieldErrors.market ? 'assetMarket-error' : 'assetMarket-hint'}><option value="">Selecione o mercado</option><option value="BR">B3</option><option value="US">Nasdaq</option></select>{fieldErrors.market ? <span id="assetMarket-error" className="field-error">{fieldErrors.market}</span> : <span id="assetMarket-hint" className="field-hint">Escolha B3 ou Nasdaq.</span>}</div><button className="primary-button" type="submit" disabled={state.status === 'loading'}>{state.status === 'loading' ? 'Pesquisando…' : 'Pesquisar ativo'}</button></form></section>
    <section className="popular-assets" aria-labelledby="popular-assets-title"><div><p className="eyebrow">Mercado em foco</p><h2 id="popular-assets-title">Ações mais movimentadas</h2><p>Escolha um ativo para preencher e consultar rapidamente.</p></div><div className="popular-assets-list">{POPULAR_ASSETS.map((asset) => <button type="button" key={asset.ticker} onClick={() => selectPopular(asset)}><strong>{asset.ticker}</strong><span>{asset.label} · {asset.market}</span></button>)}</div></section>
    <section className="asset-response" aria-live="polite">{state.status === 'initial' && <EmptyState title="Nenhuma pesquisa realizada" description="Informe um ticker para consultar os dados do ativo." />}{state.status === 'loading' && <LoadingState message={`Pesquisando ${searchedTicker}…`} />}{state.status === 'empty' && <EmptyState title="Ativo não encontrado" description={`Não há resultado disponível para ${searchedTicker}.`} />}{state.status === 'error' && <ErrorState message={state.message} onRetry={submit} />}{state.status === 'success' && <AssetResult asset={state.asset} />}</section>
  </main>
}

function AssetResult({ asset }) {
  const navigate = useNavigate(); const isUs = asset.market === 'US'
  return <article className="asset-result" aria-labelledby="asset-result-title"><header><div><p className="eyebrow">Resultado da pesquisa</p><h2 id="asset-result-title">{asset.ticker}</h2><p>{asset.name}</p></div><span className="market-badge">{asset.market}</span></header><dl className="asset-details"><Detail label="Mercado" value={isUs ? 'Estados Unidos' : 'Brasil'} /><Detail label="Moeda" value={asset.currency} /><Detail label={isUs ? 'Cotação em USD' : 'Cotação'} value={formatMoney(asset.originalPrice, asset.currency)} />{isUs && <Detail label="Valor em BRL" value={formatMoney(asset.priceBrl, 'BRL')} />}<Detail label="Horário da cotação" value={formatBrasiliaDateTime(asset.quoteQuotedAt)} />{isUs && <Detail label="Cotação USD/BRL" value={formatMoney(asset.usdBrlRate, 'BRL', false)} />}{isUs && <Detail label="Horário do USD/BRL" value={formatBrasiliaDateTime(asset.exchangeRateQuotedAt)} />}</dl><div className="freshness-warnings">{asset.quoteStale && <aside className="stale-warning" role="status"><strong>Preço de fechamento</strong><p>A cotação em tempo real não está disponível. Usando o preço de fechamento observado em {formatBrasiliaDateTime(asset.quoteQuotedAt)}.</p></aside>}{isUs && asset.exchangeRateStale && <aside className="stale-warning" role="status"><strong>USD/BRL desatualizado</strong><p>A conversão foi mantida como recebida. Última cotação observada em {formatBrasiliaDateTime(asset.exchangeRateQuotedAt)}.</p></aside>}</div><button className="primary-button asset-detail-action" type="button" onClick={() => navigate(`/app/bolsa/${asset.market}/${asset.ticker}`)}>Ver detalhes</button></article>
}
function Detail({ label, value }) { return <div><dt>{label}</dt><dd>{value}</dd></div> }

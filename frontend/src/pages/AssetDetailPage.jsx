import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { searchAsset } from '../api/market.js'
import { ErrorState, LoadingState, Message } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatBrasiliaDateTime, formatCurrency, formatMoney } from '../utils/formatters.js'

export default function AssetDetailPage() {
  const { market, ticker } = useParams(); const navigate = useNavigate(); const auth = useAuth()
  const [state, setState] = useState({ status: 'loading', asset: null, message: '' })
  const load = useCallback(async () => {
    setState({ status: 'loading', asset: null, message: '' })
    try { const asset = await searchAsset(ticker, market); setState(asset ? { status: 'ready', asset, message: '' } : { status: 'empty', asset: null, message: 'Ativo não encontrado.' }) }
    catch (error) { if (error?.status === 401) { auth.clear(); navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } }) } else setState({ status: 'error', asset: null, message: error?.message || 'Não foi possível carregar o ativo.' }) }
  }, [auth, market, navigate, ticker])
  useEffect(() => {
    const initialLoad = window.setTimeout(load, 0)
    return () => window.clearTimeout(initialLoad)
  }, [load])
  return <main className="asset-detail-page"><button className="text-button" type="button" onClick={() => navigate(-1)}>← Voltar</button>
    {state.status === 'loading' && <LoadingState message="Carregando dados oficiais do ativo…" />}
    {state.status === 'error' && <ErrorState message={state.message} onRetry={load} />}
    {state.status === 'empty' && <Message kind="error">{state.message}</Message>}
    {state.status === 'ready' && <Detail asset={state.asset} onTrade={() => navigate('/app/operacoes', { state: { asset: state.asset } })} />}
  </main>
}

function Detail({ asset, onTrade }) {
  const priceBrl = asset.priceBrl ?? asset.quotePriceBrl
  return <article className="investment-asset-detail"><header><div><p className="eyebrow">{asset.market === 'US' ? 'Ações internacionais' : 'Ações nacionais'}</p><h1>{asset.ticker}</h1><p>{asset.name}</p></div><span className="market-badge">{asset.market} · {asset.currency}</span></header><dl className="asset-details"><Item label="Cotação oficial" value={formatMoney(asset.originalPrice ?? asset.quotePrice, asset.currency)} /><Item label="Cotação em reais" value={priceBrl == null ? 'Indisponível' : formatCurrency(priceBrl)} /><Item label="Instante da cotação" value={formatBrasiliaDateTime(asset.quoteQuotedAt)} />{asset.market === 'US' && <Item label="Cotação USD/BRL" value={formatMoney(asset.usdBrlRate, 'BRL', false)} />}</dl>{(asset.quoteStale || asset.exchangeRateStale) && <Message kind="warning">Dados desatualizados. Os valores e instantes exibidos são os recebidos da API.</Message>}<button className="primary-button" type="button" onClick={onTrade} disabled={priceBrl == null}>Negociar</button></article>
}
function Item({ label, value }) { return <div><dt>{label}</dt><dd>{value}</dd></div> }

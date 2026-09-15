import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getWalletPositions } from '../api/wallet.js'
import { ErrorState, LoadingState, EmptyState } from '../components/common/AsyncStates.jsx'
import { useAuth } from '../context/auth-context.js'
import { formatCurrency, formatMoney } from '../utils/formatters.js'
import { exportPortfolioXlsx } from '../utils/portfolio-export.js'

export default function PortfolioPage() {
  const { clear } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState({ status: 'loading', snapshot: null, message: '' })

  const load = useCallback(async () => {
    setState({ status: 'loading', snapshot: null, message: '' })
    try {
      const snapshot = await getWalletPositions()
      setState({ status: 'ready', snapshot, message: '' })
    } catch (error) {
      if (error?.status === 401) {
        clear()
        navigate('/login', { replace: true })
      } else setState({ status: 'error', snapshot: null, message: error?.message || 'Não foi possível carregar sua carteira.' })
    }
  }, [clear, navigate])

  useEffect(() => { async function loadInitialData() { await Promise.resolve(); await load() }; loadInitialData() }, [load])

  if (state.status === 'loading') return <main className="private-page portfolio-page"><LoadingState message="Carregando carteira…" /></main>
  if (state.status === 'error') return <main className="private-page portfolio-page"><ErrorState message={state.message} onRetry={load} /></main>

  const positions = state.snapshot?.positions ?? []
  return <main className="private-page portfolio-page">
    <header className="portfolio-heading operations-heading"><div><p className="eyebrow">Seus investimentos</p><h1>Carteira</h1><p>Consulte os ativos mantidos em suas corretoras.</p></div><button className="secondary-button portfolio-export" type="button" onClick={() => exportPortfolioXlsx(positions)} disabled={!positions.length}><span aria-hidden="true">⇩</span> Exportar .xlsx</button></header>
    <section className="positions-section" aria-labelledby="portfolio-positions-title">
      <header><div><p className="eyebrow">Fonte oficial</p><h2 id="portfolio-positions-title">Ativos da carteira</h2></div><p>Saldo disponível <strong>{formatCurrency(state.snapshot?.availableBalance)}</strong></p></header>
      {positions.length === 0 ? <EmptyState title="Sua carteira ainda está vazia" description="Adicione um ativo pela área de Ativos para começar a acompanhar sua carteira." /> : <div className="positions-table" role="table" aria-label="Ativos da carteira"><div className="positions-table-head" role="row"><span role="columnheader">Ticker</span><span role="columnheader">Quantidade</span><span role="columnheader">Saldo / posição</span><span role="columnheader">Preço atual</span><span role="columnheader">Preço médio</span><span role="columnheader">Valorização</span></div><ul className="positions-list">{positions.map((position) => <li key={`${position.assetId}-${position.brokerageId}`}><button className="position-row" type="button" onClick={() => navigate(`/app/bolsa/${position.market}/${position.ticker}`)}><span className="position-cell position-ticker"><strong>{position.ticker}</strong><small>{position.name} · {position.market} · {position.brokerageName}</small></span><span className="position-cell"><strong>{position.quantity}</strong></span><span className="position-cell"><strong>{position.marketValueBrl == null ? 'Indisponível' : formatCurrency(position.marketValueBrl)}</strong><small>Valor da posição</small></span><span className="position-cell"><strong>{currentPrice(position)}</strong>{position.market === 'US' && <small>({formatCurrency(position.quotePriceBrl)})</small>}</span><span className="position-cell"><strong>{averagePrice(position)}</strong>{position.market === 'US' && <small>({formatCurrency(position.averagePriceBrl)})</small>}</span><span className={'position-cell ' + (Number(position.unrealizedResultBrl) >= 0 ? 'positive-value' : 'negative-value')}><strong>{position.unrealizedResultBrl == null ? 'Indisponível' : formatCurrency(position.unrealizedResultBrl)}</strong><small>{profitability(position)}</small></span></button></li>)}</ul></div>}
    </section>
  </main>
}

function currentPrice(position) { return position.quotePrice == null ? 'Indisponível' : position.market === 'US' ? formatMoney(position.quotePrice, 'USD') : formatCurrency(position.quotePriceBrl) }
function averagePrice(position) { if (position.averagePriceBrl == null) return 'Indisponível'; if (position.market !== 'US') return formatCurrency(position.averagePriceBrl); const rate = Number(position.usdBrlRate); return rate > 0 ? formatMoney(Number(position.averagePriceBrl) / rate, 'USD') : formatCurrency(position.averagePriceBrl) }
function profitability(position) { const result = Number(position.unrealizedResultBrl); const invested = Number(position.averagePriceBrl) * Number(position.quantity); return Number.isFinite(result) && invested > 0 ? `${(result / invested * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%` : 'Percentual indisponível' }

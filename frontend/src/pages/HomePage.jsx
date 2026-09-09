import { useAuth } from '../context/auth-context.js'
import { Link } from 'react-router-dom'
export default function HomePage() {
  const auth = useAuth()
  return <main className="foundation-home">
    <header className="home-hero">
      <div>
        <p className="eyebrow">Visão geral da conta</p>
        <h1>Olá, {auth.account.name}.</h1>
        <p>Organize sua carteira, acompanhe o patrimônio e registre suas decisões de investimento.</p>
      </div>
      <div className="home-session-badge"><span aria-hidden="true">●</span><div><strong>Sessão ativa</strong><small>Dados protegidos</small></div></div>
    </header>
    <section className="home-balance-strip" aria-label="Resumo da conta">
      <div><span>Saldo compartilhado</span><strong>{auth.account.balance == null ? 'Consultar no Banco' : `R$ ${Number(auth.account.balance).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`}</strong></div>
      <Link className="home-balance-action" to="/app/banco">Abrir Banco <span aria-hidden="true">→</span></Link>
    </section>
    <section className="home-contexts" aria-labelledby="home-contexts-title">
      <div className="home-section-heading"><div><p className="eyebrow">Navegação principal</p><h2 id="home-contexts-title">Escolha um contexto</h2></div><span>3 áreas</span></div>
      <nav className="home-section-links" aria-label="Seções principais">
        <Link className="home-context-card home-context-investments" to="/app/investimentos"><span className="home-context-icon" aria-hidden="true">↗</span><strong>Investimentos</strong><span>Patrimônio, carteira, resultados e histórico.</span><b>Ver investimentos <span aria-hidden="true">→</span></b></Link>
        <Link className="home-context-card home-context-bank" to="/app/banco"><span className="home-context-icon" aria-hidden="true">R$</span><strong>Banco</strong><span>Saldo compartilhado, aportes e retiradas.</span><b>Gerenciar saldo <span aria-hidden="true">→</span></b></Link>
        <Link className="home-context-card home-context-market" to="/app/bolsa"><span className="home-context-icon" aria-hidden="true">⌁</span><strong>Bolsa</strong><span>Pesquise ativos e registre compra ou venda.</span><b>Explorar ativos <span aria-hidden="true">→</span></b></Link>
      </nav>
    </section>
  </main>
}

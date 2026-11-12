import { Link } from 'react-router-dom'
import { useAuth } from '../context/auth-context.js'
import { Badge, Button, PageShell, SectionHeader, SurfaceCard } from '../components/ui/index.jsx'

export default function HomePage() {
  const auth = useAuth()
  return <PageShell className="foundation-home investor-home">
    <SectionHeader
      eyebrow="Visão geral da conta"
      title={`Olá, ${auth.account.name}.`}
      description="Organize sua carteira, acompanhe o patrimônio e registre suas decisões de investimento."
      actions={<Badge tone="success">Sessão ativa · dados protegidos</Badge>}
    />
    <SurfaceCard className="home-balance-strip" tone="raised" aria-label="Resumo da conta">
      <div><span>Saldo compartilhado</span><strong>{auth.account.balance == null ? 'Consultar no Banco' : `R$ ${Number(auth.account.balance).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`}</strong></div>
      <Button as={Link} className="home-balance-action" to="/app/banco">Abrir Banco <span aria-hidden="true">→</span></Button>
    </SurfaceCard>
    <section className="home-contexts" aria-labelledby="home-contexts-title">
      <SectionHeader eyebrow="Navegação principal" title="Escolha um contexto" titleId="home-contexts-title" actions={<Badge>3 áreas</Badge>} />
      <nav className="home-section-links" aria-label="Seções principais">
        <Link className="home-context-card home-context-investments" to="/app/investimentos"><span className="home-context-icon" aria-hidden="true">↗</span><strong>Investimentos</strong><span>Patrimônio, carteira, resultados e histórico.</span><b>Ver investimentos <span aria-hidden="true">→</span></b></Link>
        <Link className="home-context-card home-context-bank" to="/app/banco"><span className="home-context-icon" aria-hidden="true">R$</span><strong>Banco</strong><span>Saldo compartilhado, aportes e retiradas.</span><b>Gerenciar saldo <span aria-hidden="true">→</span></b></Link>
        <Link className="home-context-card home-context-market" to="/app/bolsa"><span className="home-context-icon" aria-hidden="true">⌁</span><strong>Bolsa</strong><span>Pesquise ativos e registre compra ou venda.</span><b>Explorar ativos <span aria-hidden="true">→</span></b></Link>
      </nav>
    </section>
  </PageShell>
}

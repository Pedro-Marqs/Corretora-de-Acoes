import { Badge, Button, PageShell, SectionHeader, SurfaceCard } from './ui/index.jsx'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function formatBalance(balance) {
  const numericBalance = Number(balance)
  return Number.isFinite(numericBalance) ? currencyFormatter.format(numericBalance) : 'Saldo indisponível'
}

function formatStatus(status) { return status === 'ACTIVE' ? 'Ativa' : 'Status indisponível' }

export default function AccountHome({ account, onRestart, onGoToLogin }) {
  return <PageShell className="session-home account-home">
    <header className="session-header">
      <a className="auth-brand auth-brand--dark" href="/" aria-label="Carteira Clara — início">
        <span className="auth-brand__mark" aria-hidden="true">C</span><span>Carteira Clara</span>
      </a>
      <Badge tone={account.status === 'ACTIVE' ? 'success' : 'neutral'}>{formatStatus(account.status)}</Badge>
    </header>

    <section className="session-welcome" aria-labelledby="account-title">
      <SectionHeader titleId="account-title" eyebrow="Conta criada com sucesso" title={`Olá, ${account.name}.`} description="Este é o ponto de partida da sua simulação de investimentos." />
      <div className="session-actions"><Button type="button" onClick={onGoToLogin}>Ir para login</Button><Button variant="secondary" type="button" onClick={onRestart}>Cadastrar outra conta</Button></div>
    </section>

    <SurfaceCard className="session-balance" aria-labelledby="balance-title">
      <div><p id="balance-title">Saldo disponível</p><strong>{formatBalance(account.balance)}</strong></div>
      <span className="balance-card-icon" aria-hidden="true">↗</span>
    </SurfaceCard>

    <aside className="account-notice" role="note">
      <span aria-hidden="true">i</span>
      <p><strong>Visualização inicial</strong>Esta tela usa somente os dados retornados na criação da conta. Os dados e recursos privados não são apresentados aqui.</p>
    </aside>
  </PageShell>
}

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

function formatStatus(status) {
  return status === 'ACTIVE' ? 'Ativa' : 'Status indisponível'
}

export default function AccountHome({ account, onRestart }) {
  return (
    <main className="account-home">
      <header className="account-header">
        <a className="brand brand-dark" href="/" aria-label="Carteira Clara — início">
          <span className="brand-mark" aria-hidden="true">C</span><span>Carteira Clara</span>
        </a>
        <span className="account-status"><span aria-hidden="true" />{formatStatus(account.status)}</span>
      </header>

      <section className="welcome-block" aria-labelledby="account-title">
        <div>
          <p className="eyebrow">Conta criada com sucesso</p>
          <h1 id="account-title">Olá, {account.name}.</h1>
          <p>Este é o ponto de partida da sua simulação de investimentos.</p>
        </div>
        <button className="secondary-button" type="button" onClick={onRestart}>Cadastrar outra conta</button>
      </section>

      <section className="balance-card" aria-labelledby="balance-title">
        <div>
          <p id="balance-title">Saldo disponível</p>
          <strong>{formatBalance(account.balance)}</strong>
        </div>
        <span className="balance-card-icon" aria-hidden="true">↗</span>
      </section>

      <aside className="account-notice" role="note">
        <span aria-hidden="true">i</span>
        <p><strong>Visualização inicial</strong>Esta tela usa somente os dados retornados na criação da conta. O acesso autenticado e as demais funcionalidades serão disponibilizados nas próximas etapas.</p>
      </aside>
    </main>
  )
}

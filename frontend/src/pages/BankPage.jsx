import BrokersPage from './BrokersPage.jsx'
import WalletPage from './WalletPage.jsx'

export default function BankPage() {
  return <main className="bank-page">
    <header className="bank-heading"><p className="eyebrow">Dinheiro e instituições</p><h1>Banco</h1><p>Administre o saldo compartilhado e as corretoras da sua conta.</p></header>
    <WalletPage embedded />
    <section className="withdrawal-card" aria-labelledby="withdrawal-title"><div><p className="eyebrow">Retirada</p><h2 id="withdrawal-title">Retirar saldo</h2><p>Esta operação ainda não está disponível. Seu saldo não será alterado.</p></div><button className="secondary-button" type="button" disabled aria-describedby="withdrawal-help">Indisponível</button><span id="withdrawal-help" className="field-hint">Não existe contrato de retirada habilitado no servidor.</span></section>
    <BrokersPage embedded />
  </main>
}

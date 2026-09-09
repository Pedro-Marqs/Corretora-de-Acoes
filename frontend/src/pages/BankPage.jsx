import BrokersPage from './BrokersPage.jsx'
import WalletPage from './WalletPage.jsx'

export default function BankPage() {
  return <main className="bank-page">
    <header className="bank-heading"><p className="eyebrow">Dinheiro e instituições</p><h1>Banco</h1><p>Administre o saldo compartilhado e as corretoras da sua conta.</p></header>
    <WalletPage embedded showWithdrawal />
    <BrokersPage embedded />
  </main>
}

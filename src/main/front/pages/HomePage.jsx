import { useAuth } from '../context/auth-context.js'
import { EmptyState } from '../components/common/AsyncStates.jsx'
export default function HomePage() {
  const auth = useAuth()
  return <main className="foundation-home"><h1>Olá, {auth.account.name}.</h1><EmptyState title="Sua área está pronta" description="Use Minha conta para consultar ou alterar suas credenciais." /></main>
}

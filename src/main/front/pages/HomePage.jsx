import { useAuth } from '../context/auth-context.js'
import { useNavigate } from 'react-router-dom'
import { logout } from '../api/auth.js'
import { EmptyState } from '../components/common/AsyncStates.jsx'
import { useRef, useState } from 'react'
export default function HomePage() {
  const auth = useAuth(); const navigate = useNavigate()
  const [message, setMessage] = useState('')
  const [pending, setPending] = useState(false)
  const leaving = useRef(false)
  async function leave() {
    if (leaving.current) return
    leaving.current = true; setPending(true); setMessage('')
    try { await logout(); auth.clear(); navigate('/login', { replace: true }) }
    catch (error) { if (error.status === 401) { auth.clear(); navigate('/login', { replace: true }) } else setMessage(error.message) }
    finally { leaving.current = false; setPending(false) }
  }
  return <main className="foundation-home"><h1>Olá, {auth.account.name}.</h1><p>{auth.account.cpf} · {auth.account.email}</p><EmptyState title="Sua área está pronta" description="As funcionalidades da carteira serão adicionadas nas próximas tarefas." />{message && <div className="error-banner" role="alert">{message}</div>}<button className="secondary-button" type="button" onClick={leave} disabled={pending}>{pending ? 'Saindo…' : 'Sair da conta'}</button></main>
}

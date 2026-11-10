import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAdminAuth } from '../context/admin-auth-context.js'

export default function AdminLayout() {
  const auth = useAdminAuth()
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')

  async function leave() {
    if (pending) return
    setPending(true)
    setMessage('')
    try {
      await auth.logout()
      navigate('/admin/login', { replace: true })
    } catch (error) {
      if (error.status === 401 || error.status === 403) navigate('/admin/login', { replace: true })
      else setMessage(error.message)
    } finally { setPending(false) }
  }

  return <div className="admin-shell">
    <header className="admin-header">
      <NavLink className="admin-brand" to="/admin"><span className="brand-mark" aria-hidden="true">C</span><span><strong>Carteira Clara</strong><small>Operação administrativa</small></span></NavLink>
      <nav className="admin-nav" aria-label="Navegação administrativa">
        <NavLink to="/admin" end>Dashboard</NavLink>
        <NavLink to="/admin/investors">Investidores</NavLink>
      </nav>
      <button className="admin-logout" type="button" onClick={leave} disabled={pending}>{pending ? 'Saindo…' : 'Encerrar sessão'}</button>
    </header>
    {message && <div className="error-banner admin-message" role="alert">{message}</div>}
    <main className="admin-content"><Outlet /></main>
  </div>
}

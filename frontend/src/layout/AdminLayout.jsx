import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { Badge, Button, StatusMessage } from '../components/ui/index.jsx'

export default function AdminLayout() {
  const auth = useAdminAuth()
  const navigate = useNavigate()
  const menuButtonRef = useRef(null)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  useEffect(() => {
    if (!mobileNavOpen) return undefined
    function handleEscape(event) {
      if (event.key !== 'Escape') return
      event.preventDefault()
      setMobileNavOpen(false)
      menuButtonRef.current?.focus()
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [mobileNavOpen])

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

  function closeMobileNav() { setMobileNavOpen(false) }

  return <div className={`admin-shell${mobileNavOpen ? ' admin-shell--nav-open' : ''}`}>
    <aside className="admin-sidebar" aria-label="Navegação administrativa">
      <NavLink className="admin-brand" to="/admin" onClick={closeMobileNav}>
        <span className="admin-brand__mark" aria-hidden="true">C</span>
        <span><strong>Carteira Clara</strong><small>Operação administrativa</small></span>
      </NavLink>
      <div className="admin-sidebar__context"><Badge tone="info">ADMIN</Badge><span>Visão agregada</span></div>
      <nav className="admin-nav" aria-label="Navegação administrativa">
        <p className="admin-nav__label">Operação</p>
        <NavLink to="/admin" end onClick={closeMobileNav}>Dashboard</NavLink>
        <NavLink to="/admin/investors" onClick={closeMobileNav}>Investidores</NavLink>
      </nav>
      <div className="admin-sidebar__footer"><span>Área restrita</span><span className="admin-sidebar__secure">JWT administrativo independente</span></div>
    </aside>
    {mobileNavOpen && <Button className="admin-sidebar-backdrop" variant="ghost" size="sm" type="button" aria-label="Fechar menu administrativo" onClick={closeMobileNav} />}
    <div className="admin-workspace">
      <header className="admin-topbar">
        <Button ref={menuButtonRef} className="admin-mobile-menu" variant="ghost" size="sm" type="button" aria-label="Abrir menu administrativo" onClick={() => setMobileNavOpen(true)}>☰</Button>
        <div><p className="admin-topbar__eyebrow">Contexto administrativo</p><h1>Painel de operação</h1></div>
        <div className="admin-topbar__actions"><Badge tone="info">Sessão protegida</Badge><Button className="admin-topbar__logout" variant="secondary" type="button" onClick={leave} disabled={pending}>{pending ? 'Saindo…' : 'Encerrar sessão'}</Button></div>
      </header>
      {message && <StatusMessage className="admin-message" kind="error" title="Não foi possível encerrar a sessão.">{message}</StatusMessage>}
      <main className="admin-content"><Outlet /></main>
    </div>
  </div>
}

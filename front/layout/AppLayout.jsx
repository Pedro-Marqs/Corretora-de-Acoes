import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { logout } from '../api/auth.js'
import { useAuth } from '../context/auth-context.js'

export default function AppLayout() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const lock = useRef(false)
  const menuRef = useRef(null)
  const menuButtonRef = useRef(null)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    function closeOutside(event) { if (!menuRef.current?.contains(event.target)) setMenuOpen(false) }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
  }, [])

  function menuKeyDown(event) {
    if (event.key === 'Escape') { event.preventDefault(); setMenuOpen(false); menuButtonRef.current?.focus() }
  }

  async function leave() {
    if (lock.current) return
    lock.current = true
    setPending(true)
    setMessage('')
    try {
      await logout()
      auth.clear()
      navigate('/login', { replace: true })
    } catch (error) {
      if (error.status === 401) {
        auth.clear()
        navigate('/login', { replace: true })
      } else setMessage(error.message)
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  return <div className="private-layout">
    <header className="private-header">
      <span className="brand brand-dark"><span className="brand-mark" aria-hidden="true">C</span>Carteira Clara</span>
      <nav aria-label="Navegação principal">
        <NavLink to="/app/investimentos" className={() => isInvestmentPath(location.pathname) ? 'active' : ''}>Investimentos</NavLink>
        <NavLink to="/app/banco" className={() => isBankPath(location.pathname) ? 'active' : ''}>Banco</NavLink>
        <NavLink to="/app/bolsa" className={() => isExchangePath(location.pathname) ? 'active' : ''}>Bolsa</NavLink>
      </nav>
      <div className="account-menu" ref={menuRef} onKeyDown={menuKeyDown}>
        <button ref={menuButtonRef} className="account-menu-trigger" type="button" aria-haspopup="menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>{auth.account?.name || 'Minha conta'}</button>
        {menuOpen && <div className="account-menu-popover" role="menu"><NavLink role="menuitem" to="/app/conta" onClick={() => setMenuOpen(false)}>Configurações</NavLink><button role="menuitem" type="button" onClick={leave} disabled={pending}>{pending ? 'Saindo…' : 'Sair'}</button></div>}
      </div>
    </header>
    {message && <div className="error-banner layout-error" role="alert">{message}</div>}
    <Outlet />
  </div>
}

function isInvestmentPath(path) { return ['/app/investimentos', '/app/dashboard', '/app/transferencias', '/app/historico'].some((item) => path.startsWith(item)) }
function isBankPath(path) { return ['/app/banco', '/app/carteira', '/app/corretoras'].some((item) => path.startsWith(item)) }
function isExchangePath(path) { return ['/app/bolsa', '/app/ativos', '/app/operacoes'].some((item) => path.startsWith(item)) }

import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { logout } from '../api/auth.js'
import { searchAsset } from '../api/market.js'
import { useAuth } from '../context/auth-context.js'
import { Button } from '../components/ui/index.jsx'

const navigationSections = [
  { label: 'Investimentos', items: [
    { label: 'Dashboard', to: '/app', end: true, icon: '◆' },
    { label: 'Carteira', to: '/app/operacoes', icon: '◇' },
  ] },
  { label: 'Banco', items: [
    { label: 'Banco', to: '/app/banco', icon: 'R$', iconClass: 'text-icon' },
    { label: 'Transferências', to: '/app/transferencias', icon: '⇄' },
  ] },
  { label: 'Bolsa', items: [
    { label: 'Ativos', to: '/app/ativos', activePaths: ['/app/ativos', '/app/bolsa'], icon: '◉' },
    { label: 'Histórico', to: '/app/historico', icon: '◷' },
  ] },
  { label: 'Configuração', items: [
    { label: 'Corretoras', to: '/app/corretoras', icon: '◌' },
    { label: 'Configurações', to: '/app/conta', icon: '⚙' },
  ] },
]

export default function AppLayout() {
  const auth = useAuth()
  const navigate = useNavigate()
  const lock = useRef(false)
  const menuRef = useRef(null)
  const menuButtonRef = useRef(null)
  const mobileMenuButtonRef = useRef(null)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [assetSearch, setAssetSearch] = useState('')
  const [assetSearchPending, setAssetSearchPending] = useState(false)

  useEffect(() => {
    function closeOutside(event) { if (!menuRef.current?.contains(event.target)) setMenuOpen(false) }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
  }, [])

  useEffect(() => {
    if (!menuOpen && !mobileSidebarOpen) return undefined
    function handleEscape(event) {
      if (event.key !== 'Escape') return
      event.preventDefault()
      if (menuOpen) {
        setMenuOpen(false)
        menuButtonRef.current?.focus()
      } else {
        setMobileSidebarOpen(false)
        mobileMenuButtonRef.current?.focus()
      }
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [menuOpen, mobileSidebarOpen])

  function menuKeyDown(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      setMenuOpen(false)
      menuButtonRef.current?.focus()
    }
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

  async function searchFromHeader(event) {
    event.preventDefault()
    const ticker = assetSearch.trim().toUpperCase().replace(/[^A-Z0-9]/g, '')
    if (!ticker || assetSearchPending) return
    setAssetSearchPending(true)
    try {
      let result = null
      for (const market of ['BR', 'US']) {
        try { result = await searchAsset(ticker, market) }
        catch (error) { if (error?.status === 401) throw error }
        if (result) {
          navigate(`/app/bolsa/${result.market}/${result.ticker}`)
          setAssetSearch('')
          setMobileSidebarOpen(false)
          return
        }
      }
      navigate(`/app/ativos?ticker=${encodeURIComponent(ticker)}&market=BR`)
      setMobileSidebarOpen(false)
    } catch (error) {
      if (error?.status === 401) {
        auth.clear()
        navigate('/login', { replace: true, state: { message: 'Sua sessão foi encerrada. Entre novamente.' } })
      }
    } finally { setAssetSearchPending(false) }
  }

  function closeMobileSidebar() { setMobileSidebarOpen(false) }

  function collapseNavigation() {
    setSidebarCollapsed(true)
    setMobileSidebarOpen(false)
  }

  function openNavigation() {
    if (sidebarCollapsed) {
      setSidebarCollapsed(false)
      setMobileSidebarOpen(isMobileViewport())
      return
    }
    setMobileSidebarOpen(true)
  }

  return <div className={`private-layout${sidebarCollapsed ? ' sidebar-collapsed' : ''}`}>
    <aside className={`app-sidebar${sidebarCollapsed ? ' is-collapsed' : ''}${mobileSidebarOpen ? ' is-mobile-open' : ''}`} aria-label="Navegação principal">
      <div className="sidebar-brand-row">
        <NavLink className="sidebar-brand" to="/app" end aria-label="Carteira Clara - Dashboard" onClick={closeMobileSidebar}>
          <span className="sidebar-logo" aria-hidden="true">C</span><span className="sidebar-brand-name">Carteira Clara</span>
        </NavLink>
        <Button className="sidebar-collapse" variant="ghost" size="sm" type="button" aria-label="Recolher menu" onClick={collapseNavigation}>‹</Button>
      </div>
      <nav className="sidebar-nav" aria-label="Navegação principal">
        {navigationSections.map((section) => <div className="sidebar-nav-section" key={section.label}>
          <p className="sidebar-section-label">{section.label}</p>
          {section.items.map((item) => <NavItem key={item.label} {...item} onNavigate={closeMobileSidebar} />)}
        </div>)}
      </nav>
      <div className="sidebar-footer"><span className="sidebar-footer-note">Carteira Clara · Investidor</span></div>
    </aside>
    {mobileSidebarOpen && <Button className="sidebar-backdrop" variant="ghost" size="sm" type="button" aria-label="Fechar menu" onClick={closeMobileSidebar} />}
    <div className="app-workspace">
      <header className="app-topbar">
        <Button className="mobile-menu-button" variant="ghost" size="sm" type="button" aria-label="Abrir menu" onClick={openNavigation} ref={mobileMenuButtonRef}>☰</Button>
        <form className="app-search" onSubmit={searchFromHeader} role="search">
          <span aria-hidden="true">⌕</span>
          <input type="search" value={assetSearch} onChange={(event) => setAssetSearch(event.target.value)} placeholder="Busque ações, FIIs, índices, ETFs e etc" aria-label="Buscar ativos" disabled={assetSearchPending} />
          <Button variant="ghost" size="sm" type="submit" aria-label="Pesquisar no cabeçalho" disabled={assetSearchPending}>↵</Button>
        </form>
        <div className="topbar-actions">
          <NavLink className="topbar-wallet" to="/app/banco" onClick={closeMobileSidebar}><span aria-hidden="true">R$</span><span>Banco</span><span aria-hidden="true">⌄</span></NavLink>
          <NavLink className="topbar-add" to="/app/ativos" onClick={closeMobileSidebar}><span aria-hidden="true">+</span> Adicionar ativo</NavLink>
          <div className="account-menu" ref={menuRef} onKeyDown={menuKeyDown}>
            <Button ref={menuButtonRef} className="account-menu-trigger" variant="ghost" type="button" aria-haspopup="menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}><span className="account-avatar" aria-hidden="true">{(auth.account?.name || 'C').slice(0, 1).toUpperCase()}</span><span className="account-menu-name">{auth.account?.name || 'Minha conta'}</span></Button>
            {menuOpen && <div className="account-menu-popover" role="menu"><NavLink role="menuitem" to="/app/conta" onClick={() => setMenuOpen(false)}>Configurações</NavLink><button role="menuitem" type="button" onClick={leave} disabled={pending}>{pending ? 'Saindo…' : 'Sair'}</button></div>}
          </div>
        </div>
      </header>
      {message && <div className="error-banner layout-error" role="alert">{message}</div>}
      <div className="app-content"><Outlet /></div>
    </div>
  </div>
}

function isMobileViewport() {
  return typeof window !== 'undefined'
    && (window.matchMedia?.('(max-width: 56.25rem)').matches ?? window.innerWidth <= 900)
}

function NavItem({ label, to, end, activePaths = [], icon, iconClass, onNavigate }) {
  const location = useLocation()
  return <NavLink className={({ isActive }) => `sidebar-nav-item${isActive || activePaths.some((path) => location.pathname.startsWith(path)) ? ' active' : ''}`} to={to} end={end} title={label} onClick={onNavigate}><span className={`sidebar-nav-icon ${iconClass || ''}`} aria-hidden="true">{icon}</span><span className="sidebar-label">{label}</span></NavLink>
}

import { useState } from 'react'
import { logout } from '../api/auth.js'

export default function SessionHome({ onLoggedOut }) {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  async function handleLogout() {
    setLoading(true); setMessage('')
    try {
      await logout()
      onLoggedOut()
    } catch (error) {
      if (error.status === 401) onLoggedOut()
      else setMessage(error.message)
    } finally { setLoading(false) }
  }

  return <main className="session-home"><section className="session-card" aria-labelledby="session-title">
    <span className="success-icon" aria-hidden="true">✓</span><p className="eyebrow">Sessão ativa</p>
    <h1 id="session-title">Login realizado.</h1>
    <p>Sua sessão foi criada com segurança. Os dados e recursos privados serão exibidos quando as próximas funcionalidades estiverem disponíveis.</p>
    {message && <div className="error-banner" role="alert">{message}</div>}
    <button className="primary-button" type="button" onClick={handleLogout} disabled={loading}>{loading ? 'Saindo…' : 'Sair da conta'}</button>
  </section></main>
}

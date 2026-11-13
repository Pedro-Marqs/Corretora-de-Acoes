import { useState } from 'react'
import { logout } from '../api/auth.js'
import { Button, PageShell, SectionHeader, StatusMessage, SurfaceCard } from './ui/index.jsx'

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

  return <PageShell className="session-home">
    <SurfaceCard className="session-card" tone="raised" aria-labelledby="session-title">
      <span className="success-icon" aria-hidden="true">✓</span>
      <SectionHeader eyebrow="Sessão ativa" titleId="session-title" title="Login realizado." description="Sua sessão foi criada com segurança. Os dados e recursos privados serão exibidos quando as próximas funcionalidades estiverem disponíveis." />
      {message && <StatusMessage kind="error" title="Não foi possível sair.">{message}</StatusMessage>}
      <Button type="button" onClick={handleLogout} loading={loading}>{loading ? 'Saindo…' : 'Sair da conta'}</Button>
    </SurfaceCard>
  </PageShell>
}

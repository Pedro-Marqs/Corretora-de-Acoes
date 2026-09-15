import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context.js'
import { ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'
export default function PublicRoute({ children }) {
  const auth = useAuth()
  const { status, refresh } = auth
  useEffect(() => { if (status === 'unknown') refresh() }, [status, refresh])
  if (status === 'unknown' || status === 'loading') return <LoadingState message="Validando sua sessão…" />
  if (status === 'error') return <ErrorState message={auth.error} onRetry={refresh} />
  return status === 'authenticated' ? <Navigate to="/app" replace /> : children
}

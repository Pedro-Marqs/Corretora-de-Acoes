import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/auth-context.js'
import { ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'

export default function PrivateRoute({ children }) {
  const auth = useAuth()
  const { status, refresh } = auth
  useEffect(() => { if (status === 'unknown') refresh() }, [status, refresh])
  if (auth.status === 'unknown' || auth.status === 'loading') return <LoadingState message="Validando sua sessão…" />
  if (auth.status === 'anonymous') return <Navigate to="/login" replace />
  if (auth.status === 'error') return <ErrorState message={auth.error} onRetry={auth.refresh} />
  return children
}

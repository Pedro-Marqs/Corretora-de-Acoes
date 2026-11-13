import { Navigate } from 'react-router-dom'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'

export default function AdminPublicRoute({ children }) {
  const auth = useAdminAuth()
  if (auth.status === 'unknown' || auth.status === 'loading') return <LoadingState message="Validando a sessão administrativa…" />
  if (auth.status === 'error') return <ErrorState message={auth.error} onRetry={auth.restore} />
  return auth.status === 'authenticated' ? <Navigate to="/admin" replace /> : children
}

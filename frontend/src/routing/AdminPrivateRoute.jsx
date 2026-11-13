import { Navigate } from 'react-router-dom'
import { useAdminAuth } from '../context/admin-auth-context.js'
import { ErrorState, LoadingState } from '../components/common/AsyncStates.jsx'

export default function AdminPrivateRoute({ children }) {
  const auth = useAdminAuth()
  if (auth.status === 'unknown' || auth.status === 'loading') return <LoadingState message="Validando a sessão administrativa…" />
  if (auth.status === 'anonymous') return <Navigate to="/admin/login" replace />
  if (auth.status === 'error') return <ErrorState message={auth.error} onRetry={auth.restore} />
  return children
}

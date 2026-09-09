import { useLocation, useNavigate } from 'react-router-dom'
import LoginForm from '../components/LoginForm.jsx'
import { useAuth } from '../context/auth-context.js'

export default function LoginPage() {
  const navigate = useNavigate(); const location = useLocation(); const auth = useAuth()
  async function authenticated() { if (await auth.refresh()) navigate('/app', { replace: true }) }
  return <LoginForm notice={location.state?.message} onAuthenticated={authenticated} onRegister={() => navigate('/cadastro')} onReactivate={() => navigate('/reativacao')} />
}

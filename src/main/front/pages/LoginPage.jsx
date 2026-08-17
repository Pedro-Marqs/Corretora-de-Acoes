import { useNavigate } from 'react-router-dom'
import LoginForm from '../components/LoginForm.jsx'
import { useAuth } from '../context/auth-context.js'

export default function LoginPage() {
  const navigate = useNavigate(); const auth = useAuth()
  async function authenticated() { if (await auth.refresh()) navigate('/app', { replace: true }) }
  return <LoginForm onAuthenticated={authenticated} onRegister={() => navigate('/cadastro')} />
}

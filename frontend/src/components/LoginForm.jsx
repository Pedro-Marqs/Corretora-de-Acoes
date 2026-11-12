import { useState } from 'react'
import { login } from '../api/auth.js'
import AuthFrame from './auth/AuthFrame.jsx'
import { Button, SectionHeader, StatusMessage, SurfaceCard, TextField } from './ui/index.jsx'

export default function LoginForm({ onAuthenticated, onRegister, onReactivate, notice }) {
  const [credentials, setCredentials] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  function update(event) {
    const { name, value } = event.target
    setCredentials((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage('')
    setFieldErrors({})
    try {
      await login(credentials)
      setCredentials({ email: '', password: '' })
      onAuthenticated()
    } catch (error) {
      setMessage(error.message)
      setFieldErrors(error.fieldErrors ?? {})
    } finally {
      setLoading(false)
    }
  }

  return <AuthFrame title="Bem-vindo de volta." description="Acesse sua carteira com os dados cadastrados.">
    <SurfaceCard as="section" className="auth-form-surface" aria-labelledby="login-title">
      <SectionHeader titleId="login-title" eyebrow="Acesse sua conta" title="Entre com seus dados" description="Use o e-mail e a senha cadastrados." />
      {notice && <StatusMessage kind="info">{notice}</StatusMessage>}
      {message && <StatusMessage kind="error" title="Não foi possível entrar.">{message}</StatusMessage>}
      <form className="auth-form" onSubmit={submit} noValidate>
        <TextField label="E-mail" id="login-email" name="email" type="email" autoComplete="email" value={credentials.email} onChange={update} required error={fieldErrors.email} />
        <TextField label="Senha" id="login-password" name="password" type="password" autoComplete="current-password" value={credentials.password} onChange={update} required error={fieldErrors.password} />
        <Button className="auth-submit" type="submit" disabled={loading}>{loading ? 'Entrando…' : 'Entrar'}</Button>
      </form>
      <div className="auth-links">
        <Button className="auth-link" variant="ghost" type="button" onClick={onRegister}>Ainda não tenho conta</Button>
        <Button className="auth-link" variant="ghost" type="button" onClick={onReactivate}>Reativar uma conta</Button>
      </div>
    </SurfaceCard>
  </AuthFrame>
}

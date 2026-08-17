import { useState } from 'react'
import { login } from '../api/auth.js'

export default function LoginForm({ onAuthenticated, onRegister }) {
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
    setLoading(true); setMessage(''); setFieldErrors({})
    try {
      await login(credentials)
      setCredentials({ email: '', password: '' })
      onAuthenticated()
    } catch (error) {
      setMessage(error.message)
      setFieldErrors(error.fieldErrors ?? {})
    } finally { setLoading(false) }
  }

  return (
    <main className="auth-page">
      <section className="login-card" aria-labelledby="login-title">
        <a className="brand brand-dark" href="/" aria-label="Carteira Clara — início"><span className="brand-mark" aria-hidden="true">C</span><span>Carteira Clara</span></a>
        <header className="form-header login-header"><p className="eyebrow">Acesse sua conta</p><h1 id="login-title">Bem-vindo de volta.</h1><p>Entre com o e-mail e a senha cadastrados.</p></header>
        {message && <div className="error-banner" role="alert">{message}</div>}
        <form onSubmit={submit} noValidate>
          <LoginField label="E-mail" name="email" errors={fieldErrors.email}><input id="login-email" name="email" type="email" autoComplete="email" value={credentials.email} onChange={update} required aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? 'login-email-error' : undefined} /></LoginField>
          <LoginField label="Senha" name="password" errors={fieldErrors.password}><input id="login-password" name="password" type="password" autoComplete="current-password" value={credentials.password} onChange={update} required aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? 'login-password-error' : undefined} /></LoginField>
          <button className="primary-button" type="submit" disabled={loading}>{loading ? 'Entrando…' : 'Entrar'}</button>
        </form>
        <button className="text-button" type="button" onClick={onRegister}>Ainda não tenho conta</button>
      </section>
    </main>
  )
}

function LoginField({ label, name, errors, children }) {
  return <div className="form-field"><label htmlFor={`login-${name}`}>{label}</label>{children}{errors ? <span className="field-error" id={`login-${name}-error`}>{errors.map((error) => <span key={error}>{error}</span>)}</span> : null}</div>
}

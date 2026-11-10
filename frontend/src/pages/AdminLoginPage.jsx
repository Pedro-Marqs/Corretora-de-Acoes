import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAdminAuth } from '../context/admin-auth-context.js'

export default function AdminLoginPage() {
  const auth = useAdminAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [pending, setPending] = useState(false)

  function update(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
    setMessage('')
  }

  async function submit(event) {
    event.preventDefault()
    const errors = {}
    if (!form.email.trim()) errors.email = ['E-mail é obrigatório.']
    else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) errors.email = ['E-mail inválido.']
    if (!form.password) errors.password = ['Senha é obrigatória.']
    if (Object.keys(errors).length) { setFieldErrors(errors); return }
    setPending(true)
    setMessage('')
    try {
      await auth.login({ email: form.email.trim(), password: form.password })
      navigate(location.state?.from?.pathname ?? '/admin', { replace: true })
    } catch (error) {
      setFieldErrors(error.fieldErrors ?? {})
      setMessage(error.message)
    } finally { setPending(false) }
  }

  return <main className="admin-login-page"><section className="admin-login-card" aria-labelledby="admin-login-title">
    <div className="admin-login-brand"><span className="brand-mark" aria-hidden="true">C</span><span>Carteira Clara</span></div>
    <p className="eyebrow">Área restrita</p><h1 id="admin-login-title">Acesso administrativo</h1>
    <p className="admin-login-intro">Entre para acompanhar a operação da corretora e gerenciar investidores.</p>
    {message && <div className="error-banner" role="alert">{message}</div>}
    <form onSubmit={submit} noValidate>
      <AdminField label="E-mail" name="email" error={fieldErrors.email}><input id="email" name="email" type="email" autoComplete="username" value={form.email} onChange={update} aria-invalid={Boolean(fieldErrors.email)} /></AdminField>
      <AdminField label="Senha" name="password" error={fieldErrors.password}><input id="password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={update} aria-invalid={Boolean(fieldErrors.password)} /></AdminField>
      <button className="primary-button" type="submit" disabled={pending}>{pending ? 'Entrando…' : 'Entrar como administrador'}</button>
    </form>
    <p className="privacy-note">A sessão administrativa usa cookies protegidos e não grava o token no navegador.</p>
  </section></main>
}

function AdminField({ label, name, error, children }) {
  return <div className="form-field"><label htmlFor={name}>{label}</label>{children}{error && <span className="field-error" id={`${name}-error`}>{error.map((item) => <span key={item}>{item}</span>)}</span>}</div>
}

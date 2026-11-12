import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAdminAuth } from '../context/admin-auth-context.js'
import AuthFrame from '../components/auth/AuthFrame.jsx'
import { Button, SectionHeader, StatusMessage, SurfaceCard, TextField } from '../components/ui/index.jsx'

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
    if (Object.keys(errors).length) {
      setFieldErrors(errors)
      return
    }
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

  return <AuthFrame admin title="Acesso administrativo" description="Acompanhe a operação da corretora e gerencie investidores com segurança.">
    <SurfaceCard as="section" className="auth-form-surface" aria-labelledby="admin-login-title">
      <SectionHeader titleId="admin-login-title" eyebrow="Área restrita" title="Entre como administrador" description="Use as credenciais administrativas da corretora." />
      {message && <StatusMessage kind="error" title="Não foi possível entrar.">{message}</StatusMessage>}
      <form className="auth-form" onSubmit={submit} noValidate>
        <TextField label="E-mail" id="admin-email" name="email" type="email" autoComplete="username" value={form.email} onChange={update} error={fieldErrors.email} required />
        <TextField label="Senha" id="admin-password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={update} error={fieldErrors.password} required />
        <Button className="auth-submit" type="submit" disabled={pending}>{pending ? 'Entrando…' : 'Entrar como administrador'}</Button>
      </form>
      <p className="privacy-note">A sessão administrativa usa cookies protegidos e não grava o token no navegador.</p>
    </SurfaceCard>
  </AuthFrame>
}

import { useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { createAccount } from './api/accounts.js'
import AccountHome from './components/AccountHome.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import PrivateRoute from './routing/PrivateRoute.jsx'
import PublicRoute from './routing/PublicRoute.jsx'
import AppLayout from './layout/AppLayout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import HomePage from './pages/HomePage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import AccountPage from './pages/AccountPage.jsx'

const initialForm = { name: '', cpf: '', email: '', password: '' }

function formatCpf(value) {
  return value.replace(/\D/g, '').slice(0, 11).replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3').replace(/\.(\d{3})(\d)/, '.$1-$2')
}

function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [createdAccount, setCreatedAccount] = useState(null)

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: name === 'cpf' ? formatCpf(value) : value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setFieldErrors({})
    setMessage('')
    try {
      setCreatedAccount(await createAccount(form))
    } catch (error) {
      setFieldErrors(error.fieldErrors ?? {})
      setMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  function restart() {
    setForm(initialForm)
    setFieldErrors({})
    setMessage('')
    setCreatedAccount(null)
  }

  function showLogin() {
    restart()
    navigate('/login')
  }

  if (createdAccount) return <AccountHome account={createdAccount} onRestart={restart} onGoToLogin={showLogin} />

  return (
    <main className="page-shell">
      <section className="brand-panel" aria-labelledby="page-title">
        <a className="brand" href="/" aria-label="Carteira Clara — início">
          <span className="brand-mark" aria-hidden="true">C</span><span>Carteira Clara</span>
        </a>
        <div className="brand-copy">
          <p className="eyebrow">Simulação de investimentos</p>
          <h1 id="page-title">Comece a organizar sua carteira.</h1>
          <p className="brand-description">Acompanhe seu saldo fictício, posições e resultados em um só lugar.</p>
        </div>
        <div className="starting-balance">
          <span className="balance-icon" aria-hidden="true">↗</span>
          <div><strong>R$ 10.000,00</strong><span>de saldo inicial para sua simulação</span></div>
        </div>
        <p className="academic-note">Ambiente acadêmico · Nenhuma operação envolve dinheiro real</p>
      </section>

      <section className="form-panel" aria-label="Cadastro de investidor">
        <div className="form-card">
          <>
              <header className="form-header">
                <p className="eyebrow">Crie sua conta</p><h2>Seus dados de acesso</h2>
                <p>Preencha os campos abaixo para começar.</p>
              </header>
              <button className="text-button top-login" type="button" onClick={showLogin}>Já tenho uma conta</button>
              {message && <div className="error-banner" role="alert">{message}</div>}
              <form onSubmit={handleSubmit} noValidate>
                <FormField label="Nome completo" name="name" error={fieldErrors.name}>
                  <input id="name" name="name" type="text" autoComplete="name" maxLength="150" value={form.name} onChange={updateField} required aria-invalid={Boolean(fieldErrors.name)} aria-describedby={fieldErrors.name ? 'name-error' : undefined} placeholder="Como aparece no seu documento" />
                </FormField>
                <FormField label="CPF" name="cpf" error={fieldErrors.cpf}>
                  <input id="cpf" name="cpf" type="text" inputMode="numeric" autoComplete="off" value={form.cpf} onChange={updateField} required aria-invalid={Boolean(fieldErrors.cpf)} aria-describedby={fieldErrors.cpf ? 'cpf-error' : undefined} placeholder="000.000.000-00" />
                </FormField>
                <FormField label="E-mail" name="email" error={fieldErrors.email}>
                  <input id="email" name="email" type="email" autoComplete="email" maxLength="254" value={form.email} onChange={updateField} required aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? 'email-error' : undefined} placeholder="voce@exemplo.com" />
                </FormField>
                <FormField label="Senha" name="password" error={fieldErrors.password} hint="Mínimo de 8 caracteres, com maiúscula, minúscula, número e símbolo.">
                  <input id="password" name="password" type="password" autoComplete="new-password" value={form.password} onChange={updateField} required aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? 'password-error' : 'password-hint'} placeholder="Crie uma senha segura" />
                </FormField>
                <button className="primary-button" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Criando conta…' : 'Criar minha conta'}</button>
                <p className="privacy-note">Seus dados são usados apenas neste ambiente local de simulação.</p>
              </form>
          </>
        </div>
      </section>
    </main>
  )
}

export default function App() {
  return <BrowserRouter><AuthProvider><Routes>
    <Route path="/" element={<Navigate to="/cadastro" replace />} />
    <Route path="/cadastro" element={<PublicRoute><RegisterPage /></PublicRoute>} />
    <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
    <Route path="/app" element={<PrivateRoute><AppLayout /></PrivateRoute>}><Route index element={<HomePage />} /><Route path="conta" element={<AccountPage />} /></Route>
    <Route path="/404" element={<NotFoundPage />} />
    <Route path="*" element={<Navigate to="/404" replace />} />
  </Routes></AuthProvider></BrowserRouter>
}

function FormField({ label, name, error, hint, children }) {
  return <div className="form-field"><label htmlFor={name}>{label}</label>{children}
    {error ? <span className="field-error" id={`${name}-error`}>{error.map((item) => <span key={item}>{item}</span>)}</span> : null}
    {!error && hint ? <span className="field-hint" id={`${name}-hint`}>{hint}</span> : null}
  </div>
}

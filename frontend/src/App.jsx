import { useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { createAccount } from './api/accounts.js'
import AccountHome from './components/AccountHome.jsx'
import AuthFrame from './components/auth/AuthFrame.jsx'
import { Button, SectionHeader, StatusMessage, SurfaceCard, TextField } from './components/ui/index.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { AdminAuthProvider } from './context/AdminAuthContext.jsx'
import AdminLayout from './layout/AdminLayout.jsx'
import AppLayout from './layout/AppLayout.jsx'
import AdminPrivateRoute from './routing/AdminPrivateRoute.jsx'
import AdminPublicRoute from './routing/AdminPublicRoute.jsx'
import PrivateRoute from './routing/PrivateRoute.jsx'
import PublicRoute from './routing/PublicRoute.jsx'
import AdminDashboardPage from './pages/AdminDashboardPage.jsx'
import AdminInvestorsPage from './pages/AdminInvestorsPage.jsx'
import AdminLoginPage from './pages/AdminLoginPage.jsx'
import AssetDetailPage from './pages/AssetDetailPage.jsx'
import AssetsPage from './pages/AssetsPage.jsx'
import AccountPage from './pages/AccountPage.jsx'
import BankPage from './pages/BankPage.jsx'
import BrokersPage from './pages/BrokersPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import HistoryPage from './pages/HistoryPage.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import OperationsPage from './pages/OperationsPage.jsx'
import PortfolioPage from './pages/PortfolioPage.jsx'
import ReactivationPage from './pages/ReactivationPage.jsx'
import TransfersPage from './pages/TransfersPage.jsx'
import WalletPage from './pages/WalletPage.jsx'

const initialForm = { name: '', cpf: '', email: '', password: '' }

function formatCpf(value) {
  return value.replace(/\D/g, '').slice(0, 11).replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3').replace(/\.(\d{3})(\d)/, '.$1-$2')
}

function RegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState(() => location.state?.fromReactivation ? { ...initialForm, cpf: location.state.cpf ?? '' } : initialForm)
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

  return <AuthFrame title="Comece a organizar sua carteira." description="Acompanhe seu saldo fictício, posições e resultados em um só lugar.">
    <SurfaceCard as="section" className="auth-form-surface" aria-label="Cadastro de investidor">
      <SectionHeader title="Seus dados de acesso" description="Preencha os campos abaixo para começar." eyebrow="Crie sua conta" />
      <Button className="auth-link auth-link--top" variant="ghost" type="button" onClick={showLogin}>Já tenho uma conta</Button>
      {location.state?.fromReactivation && <StatusMessage kind="info" className="registration-context">Você está criando uma conta independente. A conta anterior continuará inacessível.</StatusMessage>}
      {message && <StatusMessage kind="error" title="Não foi possível criar a conta.">{message}</StatusMessage>}
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <TextField label="Nome completo" id="name" name="name" type="text" autoComplete="name" maxLength="150" value={form.name} onChange={updateField} required error={fieldErrors.name} placeholder="Como aparece no seu documento" />
        <TextField label="CPF" id="cpf" name="cpf" type="text" inputMode="numeric" autoComplete="off" value={form.cpf} onChange={updateField} required error={fieldErrors.cpf} placeholder="000.000.000-00" />
        <TextField label="E-mail" id="email" name="email" type="email" autoComplete="email" maxLength="254" value={form.email} onChange={updateField} required error={fieldErrors.email} placeholder="voce@exemplo.com" />
        <TextField label="Senha" id="password" name="password" type="password" autoComplete="new-password" value={form.password} onChange={updateField} required error={fieldErrors.password} hint="Mínimo de 8 caracteres, com maiúscula, minúscula, número e símbolo." placeholder="Crie uma senha segura" />
        <Button className="auth-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Criando conta…' : 'Criar minha conta'}</Button>
        <p className="privacy-note">Seus dados são usados apenas neste ambiente local de simulação.</p>
      </form>
    </SurfaceCard>
  </AuthFrame>
}

export default function App() {
  return <BrowserRouter><AuthProvider><AdminAuthProvider><Routes>
    <Route path="/" element={<Navigate to="/cadastro" replace />} />
    <Route path="/cadastro" element={<PublicRoute><RegisterPage /></PublicRoute>} />
    <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
    <Route path="/reativacao" element={<PublicRoute><ReactivationPage /></PublicRoute>} />
    <Route path="/app" element={<PrivateRoute><AppLayout /></PrivateRoute>}><Route index element={<DashboardPage />} /><Route path="inicio" element={<HomePage />} /><Route path="investimentos" element={<DashboardPage />} /><Route path="banco" element={<BankPage />} /><Route path="bolsa" element={<AssetsPage />} /><Route path="bolsa/:market/:ticker" element={<AssetDetailPage />} /><Route path="dashboard" element={<DashboardPage />} /><Route path="ativos" element={<AssetsPage />} /><Route path="operacoes" element={<PortfolioPage />} /><Route path="negociar" element={<OperationsPage />} /><Route path="transferencias" element={<TransfersPage />} /><Route path="carteira" element={<WalletPage />} /><Route path="historico" element={<HistoryPage />} /><Route path="corretoras" element={<BrokersPage />} /><Route path="conta" element={<AccountPage />} /></Route>
    <Route path="/admin/login" element={<AdminPublicRoute><AdminLoginPage /></AdminPublicRoute>} />
    <Route path="/admin" element={<AdminPrivateRoute><AdminLayout /></AdminPrivateRoute>}><Route index element={<AdminDashboardPage />} /><Route path="investors" element={<AdminInvestorsPage />} /></Route>
    <Route path="/404" element={<NotFoundPage />} />
    <Route path="*" element={<Navigate to="/404" replace />} />
  </Routes></AdminAuthProvider></AuthProvider></BrowserRouter>
}

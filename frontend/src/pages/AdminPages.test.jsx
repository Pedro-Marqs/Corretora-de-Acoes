import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { getAdminDashboard } from '../api/admin-dashboard.js'
import { changeAdminInvestorStatus, listAdminInvestors } from '../api/admin-investors.js'
import { AdminAuthContext } from '../context/admin-auth-context.js'
import AdminLayout from '../layout/AdminLayout.jsx'
import AdminDashboardPage from './AdminDashboardPage.jsx'
import AdminInvestorsPage from './AdminInvestorsPage.jsx'
import AdminLoginPage from './AdminLoginPage.jsx'
import AdminPrivateRoute from '../routing/AdminPrivateRoute.jsx'
import AdminPublicRoute from '../routing/AdminPublicRoute.jsx'

vi.mock('../api/admin-dashboard.js', () => ({ getAdminDashboard: vi.fn() }))
vi.mock('../api/admin-investors.js', () => ({ changeAdminInvestorStatus: vi.fn(), listAdminInvestors: vi.fn() }))
afterEach(() => { cleanup(); vi.restoreAllMocks() })

const dashboard = {
  totalAccounts: 2, activeAccounts: 1, inactiveAccounts: 1, newAccounts: 2, purchaseCount: 3, saleCount: 1, purchasedShares: 20, soldShares: 5,
  purchaseVolume: { brl: 100, usd: 10, equivalentBrl: 150, conversionStatus: 'AVAILABLE', usdBrlRate: 5, rateQuotedAt: '2026-01-01T12:00:00Z' },
  saleVolume: { brl: 20, usd: 0, equivalentBrl: null, conversionStatus: 'UNAVAILABLE', usdBrlRate: null, rateQuotedAt: null },
  series: [{ date: '2026-01-01', newAccounts: 2, purchaseCount: 3, saleCount: 1 }], ranking: [{ ticker: 'PETR4', market: 'BR', tradedShares: 20 }],
}
const page = { content: [{ id: 'id-1', name: 'Ana', emailMasked: 'a***@example.com', cpfMasked: '123.***.***-00', status: 'ACTIVE', createdAt: '2026-01-01T12:00:00Z', inactivatedAt: null }], page: 0, size: 20, totalElements: 21, totalPages: 2 }
function adminValue(overrides = {}) { return { status: 'authenticated', error: '', isAuthenticated: true, clear: vi.fn(), login: vi.fn().mockResolvedValue(true), logout: vi.fn().mockResolvedValue(undefined), restore: vi.fn(), ...overrides } }
function withAuth(element, value = adminValue(), initialEntries = ['/admin']) { return render(<AdminAuthContext.Provider value={value}><MemoryRouter initialEntries={initialEntries}>{element}</MemoryRouter></AdminAuthContext.Provider>) }

describe('admin pages and routes', () => {
  beforeEach(() => { vi.clearAllMocks(); getAdminDashboard.mockResolvedValue(dashboard); listAdminInvestors.mockResolvedValue(page); changeAdminInvestorStatus.mockResolvedValue(undefined) })

  it('exibe dashboard, período, séries, ranking e indisponibilidade de câmbio', async () => {
    withAuth(<AdminDashboardPage />)
    expect(await screen.findByRole('heading', { name: 'Dashboard administrativo' })).toBeInTheDocument()
    expect(screen.getByText('Total atual de contas')).toBeInTheDocument()
    expect(screen.getByText('Contas ativas atualmente')).toBeInTheDocument()
    expect(screen.getByText('Contas inativas atualmente')).toBeInTheDocument()
    expect(screen.getByText('Novas contas no período')).toBeInTheDocument()
    expect(screen.getByText('Indisponível')).toBeInTheDocument()
    expect(screen.getByText('PETR4')).toBeInTheDocument()
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MAX' } })
    await waitFor(() => expect(getAdminDashboard).toHaveBeenCalledWith({ period: 'MAX', limit: 10 }))
  })

  it('informa quando a conversão cambial não se aplica', async () => {
    getAdminDashboard.mockResolvedValueOnce({ ...dashboard, saleVolume: { ...dashboard.saleVolume, conversionStatus: 'NOT_REQUIRED' } })
    withAuth(<AdminDashboardPage />)
    expect(await screen.findByText('Não se aplica')).toBeInTheDocument()
    expect(screen.getByText('Não há volume em USD para converter neste período.')).toBeInTheDocument()
  })

  it('mostra erro recuperável no dashboard', async () => {
    getAdminDashboard.mockRejectedValueOnce(new Error('Falha de rede'))
    withAuth(<AdminDashboardPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha de rede')
    getAdminDashboard.mockResolvedValueOnce(dashboard)
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    await waitFor(() => expect(screen.getByText('Total atual de contas')).toBeInTheDocument())
  })

  it('redireciona para login quando o dashboard perde autorização e trata dados vazios', async () => {
    getAdminDashboard.mockRejectedValueOnce({ status: 401 })
    withAuth(<Routes><Route path="/admin" element={<AdminDashboardPage />} /><Route path="/admin/login" element={<p>Login administrativo</p>} /></Routes>)
    expect(await screen.findByText('Login administrativo')).toBeInTheDocument()
    cleanup()
    getAdminDashboard.mockResolvedValueOnce({ ...dashboard, series: [], ranking: [] })
    withAuth(<AdminDashboardPage />)
    expect(await screen.findByText('Sem movimentações no período')).toBeInTheDocument()
    expect(screen.getByText('Sem ativos negociados')).toBeInTheDocument()
  })

  it('filtra, pagina e altera status de investidor', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    withAuth(<AdminInvestorsPage />)
    expect(await screen.findByText('a***@example.com')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Buscar por nome ou e-mail'), { target: { value: 'Ana' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    await waitFor(() => expect(listAdminInvestors).toHaveBeenCalledWith(expect.objectContaining({ search: 'Ana' })))
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'INACTIVE' } })
    await waitFor(() => expect(listAdminInvestors).toHaveBeenCalledWith(expect.objectContaining({ status: 'INACTIVE' })))
    fireEvent.click(screen.getByRole('button', { name: 'Desativar' }))
    await waitFor(() => expect(changeAdminInvestorStatus).toHaveBeenCalledWith('id-1', 'INACTIVE'))
    fireEvent.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(listAdminInvestors).toHaveBeenCalledWith(expect.objectContaining({ page: 1 })))
  })

  it('exibe lista vazia e não altera status quando a confirmação é cancelada', async () => {
    vi.stubGlobal('confirm', vi.fn(() => false))
    listAdminInvestors.mockResolvedValueOnce({ ...page, content: [], totalElements: 0, totalPages: 0 })
    withAuth(<AdminInvestorsPage />)
    expect(await screen.findByText('Nenhum investidor encontrado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeDisabled()
    expect(changeAdminInvestorStatus).not.toHaveBeenCalled()
  })

  it('trata conflito, rede e autorização na alteração de status', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    listAdminInvestors.mockResolvedValueOnce({ ...page, content: [{ ...page.content[0], status: 'INACTIVE', createdAt: null }], totalPages: 1 })
    changeAdminInvestorStatus.mockRejectedValueOnce({ status: 409, message: 'Conflito' })
    withAuth(<AdminInvestorsPage />)
    expect(await screen.findByText('Inativo')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ativar' }))
    expect(await screen.findByRole('status')).toHaveTextContent('A conta já está nesse estado')
    cleanup()
    listAdminInvestors.mockRejectedValueOnce(new Error('Falha de rede'))
    withAuth(<AdminInvestorsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha de rede')
    cleanup()
    listAdminInvestors.mockRejectedValueOnce({ status: 403 })
    withAuth(<Routes><Route path="/admin/investors" element={<AdminInvestorsPage />} /><Route path="/admin/login" element={<p>Login administrativo</p>} /></Routes>, adminValue(), ['/admin/investors'])
    expect(await screen.findByText('Login administrativo')).toBeInTheDocument()
  })

  it('desabilita ação duplicada enquanto a alteração está pendente', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    let resolveChange
    changeAdminInvestorStatus.mockReturnValueOnce(new Promise((resolve) => { resolveChange = resolve }))
    withAuth(<AdminInvestorsPage />)
    expect(await screen.findByText('a***@example.com')).toBeInTheDocument()
    const button = screen.getByRole('button', { name: 'Desativar' })
    fireEvent.click(button)
    expect(screen.getByRole('button', { name: 'Salvando…' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Salvando…' }))
    expect(changeAdminInvestorStatus).toHaveBeenCalledTimes(1)
    resolveChange()
  })

  it('valida login administrativo no cliente e navega após sucesso', async () => {
    const value = adminValue({ login: vi.fn().mockResolvedValue(true) })
    withAuth(<AdminLoginPage />, value, ['/admin/login'])
    fireEvent.click(screen.getByRole('button', { name: 'Entrar como administrador' }))
    expect(await screen.findByText('E-mail é obrigatório.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'admin@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'SenhaAdmin1!' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar como administrador' }))
    await waitFor(() => expect(value.login).toHaveBeenCalledWith({ email: 'admin@example.com', password: 'SenhaAdmin1!' }))
  })

  it('exibe credenciais inválidas e erros de campo no login', async () => {
    const value = adminValue({ login: vi.fn().mockRejectedValue({ message: 'Credenciais inválidas.', fieldErrors: { email: ['E-mail inválido.'] } }) })
    withAuth(<AdminLoginPage />, value, ['/admin/login'])
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'admin@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'senha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar como administrador' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciais inválidas.')
    expect(screen.getByText('E-mail inválido.')).toBeInTheDocument()
  })

  it('encerra sessão no layout administrativo', async () => {
    const value = adminValue()
    withAuth(<Routes><Route path="/admin" element={<AdminLayout />}><Route index element={<p>Conteúdo</p>} /></Route></Routes>, value)
    fireEvent.click(screen.getByRole('button', { name: 'Encerrar sessão' }))
    await waitFor(() => expect(value.logout).toHaveBeenCalled())
  })

  it('fecha a navegação administrativa mobile com Escape e devolve o foco', () => {
    const value = adminValue()
    withAuth(<Routes><Route path="/admin" element={<AdminLayout />}><Route index element={<p>Conteúdo</p>} /></Route></Routes>, value)
    const trigger = screen.getByRole('button', { name: 'Abrir menu administrativo' })
    fireEvent.click(trigger)
    expect(trigger.closest('.admin-shell')).toHaveClass('admin-shell--nav-open')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(trigger.closest('.admin-shell')).not.toHaveClass('admin-shell--nav-open')
    expect(trigger).toHaveFocus()
  })

  it('redireciona em logout 401/403 e mostra falha transitória', async () => {
    const denied = adminValue({ logout: vi.fn().mockRejectedValue({ status: 403 }) })
    withAuth(<Routes><Route path="/admin" element={<AdminLayout />}><Route index element={<p>Conteúdo</p>} /></Route><Route path="/admin/login" element={<p>Login administrativo</p>} /></Routes>, denied)
    fireEvent.click(screen.getByRole('button', { name: 'Encerrar sessão' }))
    expect(await screen.findByText('Login administrativo')).toBeInTheDocument()
    cleanup()
    const failed = adminValue({ logout: vi.fn().mockRejectedValue(new Error('Falha transitória')) })
    withAuth(<Routes><Route path="/admin" element={<AdminLayout />}><Route index element={<p>Conteúdo</p>} /></Route></Routes>, failed)
    fireEvent.click(screen.getByRole('button', { name: 'Encerrar sessão' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha transitória')
  })

  it('protege a rota privada e redireciona a rota pública autenticada', () => {
    const { rerender } = withAuth(<Routes><Route path="/admin" element={<AdminPrivateRoute><p>Privado</p></AdminPrivateRoute>} /><Route path="/admin/login" element={<p>Login</p>} /></Routes>, adminValue({ status: 'anonymous', isAuthenticated: false }), ['/admin'])
    expect(screen.getByText('Login')).toBeInTheDocument()
    rerender(<AdminAuthContext.Provider value={adminValue()}><MemoryRouter initialEntries={['/admin/login']}><Routes><Route path="/admin/login" element={<AdminPublicRoute><p>Login</p></AdminPublicRoute>} /><Route path="/admin" element={<p>Privado</p>} /></Routes></MemoryRouter></AdminAuthContext.Provider>)
    expect(screen.getByText('Privado')).toBeInTheDocument()
  })

  it('mantém loading e erro recuperável nos guards administrativos', () => {
    cleanup()
    withAuth(<AdminPrivateRoute><p>Privado</p></AdminPrivateRoute>, adminValue({ status: 'loading' }))
    expect(screen.getByRole('status')).toHaveTextContent('Validando')
    cleanup()
    const restore = vi.fn()
    withAuth(<AdminPublicRoute><p>Login</p></AdminPublicRoute>, adminValue({ status: 'error', error: 'Falha', restore }), ['/admin/login'])
    expect(screen.getByRole('alert')).toHaveTextContent('Falha')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(restore).toHaveBeenCalled()
  })
})

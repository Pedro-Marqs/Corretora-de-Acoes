import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { endAdminSession } from '../api/admin-auth-token.js'
import { loginAdmin, restoreAdminSession } from '../api/admin-auth.js'
import { useAdminAuth } from './admin-auth-context.js'
import { AdminAuthProvider } from './AdminAuthContext.jsx'

vi.mock('../api/admin-auth.js', () => ({ loginAdmin: vi.fn(), logoutAdmin: vi.fn(), restoreAdminSession: vi.fn() }))
afterEach(cleanup)

function Probe() {
  const auth = useAdminAuth()
  return <div><output data-testid="status">{auth.status}</output><output data-testid="error">{auth.error}</output><button type="button" onClick={auth.clear}>Sair</button></div>
}

describe('AdminAuthProvider', () => {
  beforeEach(() => { vi.clearAllMocks(); endAdminSession() })

  it('restaura a sessão administrativa após reload', async () => {
    restoreAdminSession.mockResolvedValue('admin-token')
    render(<AdminAuthProvider><Probe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(restoreAdminSession).toHaveBeenCalledTimes(1)
  })

  it('converte refresh inválido em sessão anônima', async () => {
    restoreAdminSession.mockRejectedValue({ status: 401 })
    render(<AdminAuthProvider><Probe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'))
  })

  it('mantém erro recuperável em falha de rede e trata 403 como anônimo', async () => {
    restoreAdminSession.mockRejectedValueOnce(new Error('Servidor indisponível'))
    render(<AdminAuthProvider><Probe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('error'))
    expect(screen.getByTestId('error')).toHaveTextContent('Servidor indisponível')
    cleanup()
    restoreAdminSession.mockRejectedValueOnce({ status: 403 })
    render(<AdminAuthProvider><Probe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'))
  })

  it('não reativa estado após limpar a sessão', async () => {
    let resolveRestore
    restoreAdminSession.mockReturnValue(new Promise((resolve) => { resolveRestore = resolve }))
    render(<AdminAuthProvider><Probe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(restoreAdminSession).toHaveBeenCalledTimes(1))
    screen.getByRole('button', { name: 'Sair' }).click()
    resolveRestore('stale-token')
    await Promise.resolve()
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
  })

  it('propaga login e mantém erro seguro', async () => {
    loginAdmin.mockRejectedValue({ message: 'Credenciais inválidas.', fieldErrors: { email: ['E-mail inválido.'] } })
    function LoginProbe() { const auth = useAdminAuth(); return <button type="button" onClick={() => { void auth.login({}).catch(() => {}) }}>Login</button> }
    render(<AdminAuthProvider><LoginProbe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(restoreAdminSession).toHaveBeenCalled())
    restoreAdminSession.mockResolvedValue('')
    screen.getByRole('button', { name: 'Login' }).click()
    await vi.waitFor(() => expect(loginAdmin).toHaveBeenCalled())
  })

  it('autentica login bem-sucedido e ignora resposta de login obsoleta', async () => {
    loginAdmin.mockResolvedValueOnce('admin-token')
    function LoginProbe() { const auth = useAdminAuth(); return <><output data-testid="login-status">{auth.status}</output><button type="button" onClick={() => { void auth.login({}) }}>Login</button><button type="button" onClick={auth.clear}>Limpar</button></> }
    render(<AdminAuthProvider><LoginProbe /></AdminAuthProvider>)
    await vi.waitFor(() => expect(restoreAdminSession).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: 'Login' }))
    await vi.waitFor(() => expect(screen.getByTestId('login-status')).toHaveTextContent('authenticated'))

    let resolveLogin
    loginAdmin.mockReturnValueOnce(new Promise((resolve) => { resolveLogin = resolve }))
    fireEvent.click(screen.getByRole('button', { name: 'Login' }))
    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }))
    resolveLogin('stale-token')
    await Promise.resolve()
    expect(screen.getByTestId('login-status')).toHaveTextContent('anonymous')
  })
})

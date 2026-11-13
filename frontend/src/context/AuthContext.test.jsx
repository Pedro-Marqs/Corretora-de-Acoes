import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCurrentAccount } from '../api/accounts.js'
import { restoreSession } from '../api/auth.js'
import { clearAccessToken } from '../api/auth-token.js'
import { useAuth } from './auth-context.js'
import { AuthProvider } from './AuthContext.jsx'

vi.mock('../api/accounts.js', () => ({ getCurrentAccount: vi.fn() }))
vi.mock('../api/auth.js', () => ({ restoreSession: vi.fn() }))

afterEach(cleanup)

function Probe() {
  const auth = useAuth()
  return <div>
    <output data-testid="status">{auth.status}</output>
    <output data-testid="error">{auth.error}</output>
    <button type="button" onClick={auth.clear}>Sign out</button>
  </div>
}

function renderProvider() {
  return render(<AuthProvider><Probe /></AuthProvider>)
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearAccessToken()
  })

  it('restores JWT before querying the current account', async () => {
    restoreSession.mockResolvedValue('access-1')
    getCurrentAccount.mockResolvedValue({ name: 'Ana Silva' })
    renderProvider()

    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(restoreSession).toHaveBeenCalledTimes(1)
    expect(getCurrentAccount).toHaveBeenCalledTimes(1)
  })

  it.each([
    ['invalid refresh', { status: 401 }],
    ['revoked refresh', { status: 403 }],
  ])('becomes anonymous after an %s', async (_label, error) => {
    restoreSession.mockRejectedValue(error)
    getCurrentAccount.mockRejectedValue({ status: 401 })
    renderProvider()

    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'))
    expect(screen.getByTestId('error')).toHaveTextContent('')
  })

  it('keeps a temporary network failure retryable', async () => {
    restoreSession.mockRejectedValue(new Error('Network unavailable'))
    renderProvider()

    await vi.waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('error'))
    expect(screen.getByTestId('error')).toHaveTextContent('Network unavailable')
  })

  it('does not reactivate a session after a delayed restore returns post-logout', async () => {
    let resolveRestore
    restoreSession.mockReturnValue(new Promise((resolve) => { resolveRestore = resolve }))
    getCurrentAccount.mockResolvedValue({ name: 'Old session' })
    renderProvider()

    await vi.waitFor(() => expect(restoreSession).toHaveBeenCalledTimes(1))
    screen.getByRole('button', { name: 'Sign out' }).click()
    resolveRestore('stale-token')
    await Promise.resolve()
    await Promise.resolve()

    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
    expect(getCurrentAccount).not.toHaveBeenCalled()
  })

  it('does not reactivate a session after a delayed account response post-logout', async () => {
    restoreSession.mockResolvedValue('access-1')
    let resolveAccount
    getCurrentAccount.mockReturnValue(new Promise((resolve) => { resolveAccount = resolve }))
    renderProvider()
    await vi.waitFor(() => expect(getCurrentAccount).toHaveBeenCalledTimes(1))

    screen.getByRole('button', { name: 'Sign out' }).click()
    resolveAccount({ name: 'Old session' })
    await Promise.resolve()

    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
  })
})

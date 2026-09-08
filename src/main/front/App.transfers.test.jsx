import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { getCurrentAccount } from './api/accounts.js'
import { getActiveBrokers } from './api/brokers.js'
import { getWalletPositions } from './api/wallet.js'

vi.mock('./api/accounts.js', async (load) => ({ ...await load(), getCurrentAccount: vi.fn() }))
vi.mock('./api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('./api/wallet.js', async (load) => ({ ...await load(), getWalletPositions: vi.fn() }))
afterEach(cleanup)

describe('transfer private route', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/app/transferencias')
    getActiveBrokers.mockResolvedValue([])
    getWalletPositions.mockResolvedValue({ availableBalance: '1000.00', positions: [] })
  })

  it('integra a página à rota e navegação privadas', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Transferir posição' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Investimentos' })).toHaveClass('active')
    expect(getWalletPositions).toHaveBeenCalledOnce()
  })

  it('não consulta nem exibe posições sem sessão', async () => {
    getCurrentAccount.mockRejectedValue({ status: 401 })
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Transferir posição' })).not.toBeInTheDocument()
    expect(getWalletPositions).not.toHaveBeenCalled()
  })
})

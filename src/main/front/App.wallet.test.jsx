import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { getCurrentAccount } from './api/accounts.js'
import { getWalletBalance } from './api/wallet.js'

vi.mock('./api/accounts.js', async (load) => {
  const actual = await load()
  return { ...actual, getCurrentAccount: vi.fn() }
})
vi.mock('./api/wallet.js', async (load) => {
  const actual = await load()
  return { ...actual, deposit: vi.fn(), getWalletBalance: vi.fn() }
})

afterEach(cleanup)

describe('wallet private route', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/app/carteira')
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    getWalletBalance.mockResolvedValue({ balance: 10000 })
  })

  it('integra a carteira à rota e à navegação privadas', async () => {
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Minha carteira' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Carteira' })).toHaveClass('active')
    expect(screen.getByText('R$ 10.000,00')).toBeInTheDocument()
    expect(getCurrentAccount).toHaveBeenCalledTimes(1)
    expect(getWalletBalance).toHaveBeenCalledTimes(1)
  })
})

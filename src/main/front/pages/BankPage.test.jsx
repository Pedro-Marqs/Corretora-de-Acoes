import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { getWalletBalance } from '../api/wallet.js'
import { AuthContext } from '../context/auth-context.js'
import BankPage from './BankPage.jsx'

vi.mock('../api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('../api/wallet.js', async (load) => ({ ...await load(), getWalletBalance: vi.fn() }))
afterEach(cleanup)

describe('BankPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    getActiveBrokers.mockResolvedValue([])
  })

  it('compõe saldo, retirada segura e corretoras sob um único landmark main', async () => {
    const { container } = render(<AuthContext.Provider value={{ clear: vi.fn() }}><MemoryRouter><BankPage /></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByText('R$ 10.000,00')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Banco' })).toBeInTheDocument()
    expect(screen.getAllByRole('main')).toHaveLength(1)
    expect(container.querySelector('.wallet-page')?.tagName).toBe('SECTION')
    expect(container.querySelector('.brokers-page')?.tagName).toBe('SECTION')
    expect(screen.getByRole('button', { name: 'Indisponível' })).toBeDisabled()
    expect(screen.getByText(/Seu saldo não será alterado/)).toBeInTheDocument()
  })
})

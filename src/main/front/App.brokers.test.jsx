import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { getCurrentAccount } from './api/accounts.js'
import { getActiveBrokers } from './api/brokers.js'

vi.mock('./api/accounts.js', async (load) => ({ ...await load(), getCurrentAccount: vi.fn() }))
vi.mock('./api/brokers.js', async (load) => ({ ...await load(), associateBroker: vi.fn(), getActiveBrokers: vi.fn(), removeBroker: vi.fn(), searchBroker: vi.fn() }))
afterEach(cleanup)

describe('brokers private route', () => {
  beforeEach(() => { vi.clearAllMocks(); window.history.replaceState({}, '', '/app/corretoras'); getActiveBrokers.mockResolvedValue([]) })
  it('integra a página à rota e navegação privadas', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' }); render(<App />)
    expect(await screen.findByRole('heading', { name: 'Minhas corretoras' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Corretoras' })).toHaveClass('active')
    expect(getCurrentAccount).toHaveBeenCalledTimes(1); expect(getActiveBrokers).toHaveBeenCalledTimes(1)
  })
  it('não expõe a página sem autenticação', async () => {
    getCurrentAccount.mockRejectedValue({ status: 401 }); render(<App />)
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Minhas corretoras' })).not.toBeInTheDocument()
    expect(getActiveBrokers).not.toHaveBeenCalled()
  })
})

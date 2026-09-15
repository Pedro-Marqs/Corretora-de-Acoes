import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { deleteAccount, getCurrentAccount } from './api/accounts.js'

vi.mock('./api/accounts.js', async (importOriginal) => ({
  ...await importOriginal(),
  deleteAccount: vi.fn(),
  getCurrentAccount: vi.fn(),
}))

afterEach(cleanup)

describe('ciclo autenticado da conta com rotas reais', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/app/conta')
    getCurrentAccount.mockResolvedValue({ name: 'Ana Silva', cpf: '529.***.***-25', email: 'a***@example.com' })
    deleteAccount.mockResolvedValue()
  })

  it('limpa a sessão antes da rota pública e preserva a mensagem após excluir', async () => {
    render(<App />)
    await screen.findByRole('heading', { name: 'Minha conta' })

    fireEvent.click(screen.getByRole('button', { name: 'Excluir minha conta' }))
    fireEvent.change(screen.getByLabelText(/E-mail atual/), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText(/Senha atual/, { selector: '#deletePassword' }), { target: { value: 'Senha@123' } })
    fireEvent.change(screen.getByLabelText(/Digite "Excluir"/), { target: { value: 'Excluir' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Conta excluída. Seus dados permanecem preservados')
    await waitFor(() => expect(window.location.pathname).toBe('/login'))
    expect(screen.getByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Minha conta' })).not.toBeInTheDocument()
    expect(deleteAccount).toHaveBeenCalledTimes(1)
  })
})

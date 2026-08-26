import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../context/auth-context.js'
import { AccountApiError, changeEmail, changePassword, deleteAccount, getCurrentAccount } from '../api/accounts.js'
import AccountPage from './AccountPage.jsx'

vi.mock('../api/accounts.js', async (original) => ({ ...await original(), changeEmail: vi.fn(), changePassword: vi.fn(), deleteAccount: vi.fn(), getCurrentAccount: vi.fn() }))
afterEach(() => { cleanup(); vi.clearAllMocks() })

const account = { name: 'Ana Silva', cpf: '529.***.***-25', email: 'a***@example.com' }
function setup(overrides = {}) { const value = { account, clear: vi.fn(), refresh: vi.fn().mockResolvedValue(true), ...overrides }; render(<AuthContext.Provider value={value}><MemoryRouter initialEntries={['/app/conta']}><Routes><Route path="/app/conta" element={<AccountPage />} /><Route path="/login" element={<div>Login destino</div>} /></Routes></MemoryRouter></AuthContext.Provider>); return value }
function openEmail() { fireEvent.click(screen.getByRole('button', { name: 'Alterar e-mail' })) }
function openPassword() { fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' })) }
function openDeletion() { fireEvent.click(screen.getByRole('button', { name: 'Excluir minha conta' })) }

describe('AccountPage', () => {
  it('exibe dados mascarados com os formulários inicialmente recolhidos', () => {
    setup()
    expect(screen.getByText('529.***.***-25')).toBeInTheDocument()
    expect(screen.getByText('a***@example.com')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Alterar e-mail' })).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByRole('button', { name: 'Alterar senha' })).toHaveAttribute('aria-expanded', 'false')
    expect(document.querySelectorAll('input[required]')).toHaveLength(0)
  })

  it('expande apenas o formulário escolhido abaixo do título', () => {
    setup(); openEmail()
    expect(screen.getByLabelText(/Novo e-mail/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/Nova senha/)).not.toBeInTheDocument()
    openPassword()
    expect(screen.queryByLabelText(/Novo e-mail/)).not.toBeInTheDocument()
    expect(screen.getByLabelText(/Nova senha/)).toBeInTheDocument()
  })

  it('altera e-mail, limpa auth e redireciona para login', async () => {
    changeEmail.mockResolvedValue(); const auth = setup(); openEmail()
    fireEvent.change(screen.getByLabelText(/Novo e-mail/), { target: { value: 'novo@example.com' } })
    fireEvent.change(screen.getByLabelText(/Senha atual/), { target: { value: 'Atual@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alteração' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(changeEmail).toHaveBeenCalledWith({ newEmail: 'novo@example.com', currentPassword: 'Atual@123' })
    expect(auth.clear).toHaveBeenCalled()
  })

  it('mantém erros de senha isolados quando a sessão continua válida', async () => {
    changePassword.mockRejectedValue(new AccountApiError('Dados inválidos.', { currentPassword: ['Senha atual incorreta.'], newPassword: ['Senha deve conter número.'] }, 401)); getCurrentAccount.mockResolvedValue(account)
    setup(); openPassword(); fireEvent.click(screen.getByRole('button', { name: 'Salvar alteração' }))
    expect(await screen.findByText('Senha deve conter número.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Nova senha/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getAllByText('Senha atual incorreta.')).toHaveLength(1)
  })

  it('redireciona quando a consulta confirma sessão expirada', async () => {
    changeEmail.mockRejectedValue(new AccountApiError('Sessão inválida.', {}, 401)); getCurrentAccount.mockRejectedValue(new AccountApiError('Sessão inválida.', {}, 401))
    const auth = setup(); openEmail(); fireEvent.click(screen.getByRole('button', { name: 'Salvar alteração' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument(); expect(auth.clear).toHaveBeenCalled()
  })

  it('oferece retry se a validação da sessão falha tecnicamente', async () => {
    changeEmail.mockRejectedValue(new AccountApiError('Não autorizado.', {}, 401)); getCurrentAccount.mockRejectedValueOnce(new AccountApiError('Servidor indisponível.', {}, 500)).mockResolvedValueOnce(account)
    setup(); openEmail(); fireEvent.click(screen.getByRole('button', { name: 'Salvar alteração' }))
    expect(await screen.findByText('Servidor indisponível.')).toBeInTheDocument(); fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Não autorizado.')
  })

  it('bloqueia duplo envio de credencial', () => {
    changePassword.mockReturnValue(new Promise(() => {})); setup(); openPassword()
    const button = screen.getByRole('button', { name: 'Salvar alteração' }); fireEvent.click(button); fireEvent.click(button)
    expect(changePassword).toHaveBeenCalledTimes(1); expect(screen.getByRole('button', { name: 'Salvando…' })).toBeDisabled()
  })

  it('abre a exclusão em modal com o texto entre aspas e restaura o foco', async () => {
    setup(); const trigger = screen.getByRole('button', { name: 'Excluir minha conta' }); trigger.focus(); openDeletion()
    const dialog = screen.getByRole('dialog', { name: 'Excluir minha conta' })
    expect(screen.getByLabelText(/Digite "Excluir"/)).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Cancelar' })).toHaveFocus())
    fireEvent.keyDown(dialog, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(); expect(trigger).toHaveFocus()
  })

  it('exige os três campos da modal antes de chamar a API', async () => {
    setup(); openDeletion(); fireEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))
    expect(await screen.findByText('E-mail atual é obrigatório.')).toBeInTheDocument()
    expect(screen.getByText('Senha atual é obrigatória.')).toBeInTheDocument()
    expect(screen.getByText('Digite "Excluir" para confirmar.')).toBeInTheDocument()
    expect(deleteAccount).not.toHaveBeenCalled()
  })

  it('impede exclusão quando a confirmação não é exatamente Excluir', async () => {
    setup(); openDeletion(); fillDeletion('excluir'); fireEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))
    expect(await screen.findByText('A confirmação deve ser exatamente "Excluir".')).toBeInTheDocument(); expect(deleteAccount).not.toHaveBeenCalled()
  })

  it('exclui, limpa a autenticação e redireciona para o login', async () => {
    deleteAccount.mockResolvedValue(); const auth = setup(); openDeletion(); fillDeletion('Excluir'); fireEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(deleteAccount).toHaveBeenCalledWith({ email: 'ana@example.com', password: 'Senha@123', confirmation: 'Excluir' }); expect(auth.clear).toHaveBeenCalled()
  })

  it('preserva os dados e apresenta erros funcionais da exclusão', async () => {
    deleteAccount.mockRejectedValue(new AccountApiError('Confirmação inválida.', { password: ['Senha atual incorreta.'] }, 400))
    setup(); openDeletion(); fillDeletion('Excluir'); fireEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Confirmação inválida.')
    expect(screen.getByText('Senha atual incorreta.')).toBeInTheDocument(); expect(screen.getByLabelText(/E-mail atual/)).toHaveValue('ana@example.com')
  })

  it('bloqueia múltiplas exclusões enquanto a solicitação está pendente', () => {
    deleteAccount.mockReturnValue(new Promise(() => {})); setup(); openDeletion(); fillDeletion('Excluir')
    const button = screen.getByRole('button', { name: 'Confirmar exclusão' }); fireEvent.click(button); fireEvent.click(button)
    expect(deleteAccount).toHaveBeenCalledTimes(1); expect(screen.getByRole('button', { name: 'Excluindo conta…' })).toBeDisabled()
  })
})

function fillDeletion(confirmation) {
  fireEvent.change(screen.getByLabelText(/E-mail atual/), { target: { value: 'ana@example.com' } })
  fireEvent.change(screen.getByLabelText(/Senha atual/), { target: { value: 'Senha@123' } })
  fireEvent.change(screen.getByLabelText(/Digite "Excluir"/), { target: { value: confirmation } })
}

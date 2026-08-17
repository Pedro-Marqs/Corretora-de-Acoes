import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { AccountApiError, createAccount, getCurrentAccount } from './api/accounts.js'
import { AuthApiError, login, logout } from './api/auth.js'

vi.mock('./api/accounts.js', async (importOriginal) => {
  const original = await importOriginal()
  return { ...original, createAccount: vi.fn(), getCurrentAccount: vi.fn() }
})
vi.mock('./api/auth.js', async (importOriginal) => {
  const original = await importOriginal()
  return { ...original, login: vi.fn(), logout: vi.fn() }
})
vi.mock('./routing/PublicRoute.jsx', () => ({ default: ({ children }) => children }))

afterEach(cleanup)

function fillForm() {
  fireEvent.change(screen.getByLabelText('Nome completo'), { target: { value: 'Ana Silva' } })
  fireEvent.change(screen.getByLabelText('CPF'), { target: { value: '52998224725' } })
  fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
  fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
}

describe('Cadastro', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/cadastro')
    getCurrentAccount.mockResolvedValue({ name: 'Ana Silva', cpf: '529.***.***-25', email: 'a***@example.com' })
  })

  it('exibe todos os campos e a informação do saldo inicial', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Comece a organizar sua carteira.' })).toBeInTheDocument()
    expect(screen.getByText('R$ 10.000,00')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar minha conta' })).toBeEnabled()
  })

  it('envia os dados e apresenta a tela inicial apenas com o retorno público', async () => {
    createAccount.mockResolvedValue({ name: 'Ana Silva', balance: 10000, status: 'ACTIVE' })
    render(<App />)
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Criar minha conta' }))

    await screen.findByRole('heading', { name: 'Olá, Ana Silva.' })
    expect(createAccount).toHaveBeenCalledWith({ name: 'Ana Silva', cpf: '529.982.247-25', email: 'ana@example.com', password: 'Senha@123' })
    expect(screen.getByText('R$ 10.000,00')).toBeInTheDocument()
    expect(screen.getByText('Ativa')).toBeInTheDocument()
    expect(screen.getByText(/Esta tela usa somente os dados retornados/)).toBeInTheDocument()
    expect(screen.queryByText('529.982.247-25')).not.toBeInTheDocument()
    expect(screen.queryByText('ana@example.com')).not.toBeInTheDocument()
    expect(screen.queryByText(/corretora/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/histórico/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ir para login' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /comprar|vender/i })).not.toBeInTheDocument()
  })

  it('usa mensagens seguras quando saldo e status retornados são desconhecidos', async () => {
    createAccount.mockResolvedValue({ name: 'Ana Silva', balance: 'inválido', status: 'UNKNOWN' })
    render(<App />)
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Criar minha conta' }))

    expect(await screen.findByText('Saldo indisponível')).toBeInTheDocument()
    expect(screen.getByText('Status indisponível')).toBeInTheDocument()
  })

  it('permite voltar ao formulário sem persistir o retorno da conta', async () => {
    createAccount.mockResolvedValue({ name: 'Ana Silva', balance: 10000, status: 'ACTIVE' })
    render(<App />)
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Cadastrar outra conta' }))

    expect(screen.getByRole('button', { name: 'Criar minha conta' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nome completo')).toHaveValue('')
  })

  it('mostra múltiplos erros nos campos e permite corrigi-los', async () => {
    createAccount.mockRejectedValue(new AccountApiError('Os dados informados são inválidos.', { cpf: ['CPF inválido.'], email: ['E-mail inválido.'] }))
    render(<App />)
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Criar minha conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Os dados informados são inválidos.')
    expect(screen.getByText('CPF inválido.')).toBeInTheDocument()
    expect(screen.getByText('E-mail inválido.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('CPF'), { target: { value: '123' } })
    await waitFor(() => expect(screen.queryByText('CPF inválido.')).not.toBeInTheDocument())
  })

  it('bloqueia reenvio enquanto aguarda a API', async () => {
    createAccount.mockReturnValue(new Promise(() => {}))
    render(<App />)
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    expect(screen.getByRole('button', { name: 'Criando conta…' })).toBeDisabled()
  })

  it('realiza login e logout sem exibir dados privados inventados', async () => {
    login.mockResolvedValue(); logout.mockResolvedValue()
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Olá, Ana Silva.' })).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({ email: 'ana@example.com', password: 'Senha@123' })
    expect(screen.queryByText(/saldo disponível/i)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Sair da conta' }))
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
  })

  it('mantém a sessão visual quando o logout falha', async () => {
    login.mockResolvedValue(); logout.mockRejectedValue(new Error('Não foi possível encerrar a sessão.'))
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Sair da conta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível encerrar a sessão.')
    expect(screen.getByRole('heading', { name: 'Olá, Ana Silva.' })).toBeInTheDocument()
  })

  it('remove o estado visual autenticado quando logout retorna 401', async () => {
    login.mockResolvedValue(); logout.mockRejectedValue(new AuthApiError('Sessão inválida.', {}, 401))
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Sair da conta' }))

    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Olá, Ana Silva.' })).not.toBeInTheDocument()
  })

  it('limpa o formulário de cadastro ao alternar para login e voltar', () => {
    render(<App />)
    fireEvent.change(screen.getByLabelText('Nome completo'), { target: { value: 'Ana Silva' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ainda não tenho conta' }))

    expect(screen.getByLabelText('Nome completo')).toHaveValue('')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
  })

  it('mostra erro neutro e erros estruturais no login', async () => {
    login.mockRejectedValue(new AuthApiError('Credenciais inválidas.', { email: ['E-mail inválido.'] }))
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciais inválidas.')
    expect(screen.getByText('E-mail inválido.')).toBeInTheDocument()
    expect(screen.queryByText(/senha incorreta|e-mail inexistente/i)).not.toBeInTheDocument()
  })

  it('impede reenvio enquanto o login está em andamento', () => {
    login.mockReturnValue(new Promise(() => {}))
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(screen.getByRole('button', { name: 'Entrando…' })).toBeDisabled()
  })

  it('impede requisições duplicadas no logout', async () => {
    login.mockResolvedValue(); getCurrentAccount.mockResolvedValue({ name: 'Ana Silva', cpf: '529.***.***-25', email: 'a***@example.com' })
    logout.mockReturnValue(new Promise(() => {}))
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Já tenho uma conta' }))
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    const button = await screen.findByRole('button', { name: 'Sair da conta' })
    fireEvent.click(button); fireEvent.click(button)
    expect(logout).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Saindo…' })).toBeDisabled()
  })
})

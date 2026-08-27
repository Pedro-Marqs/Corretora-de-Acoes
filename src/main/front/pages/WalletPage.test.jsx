import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { deposit, WalletApiError, getWalletBalance } from '../api/wallet.js'
import { AuthContext } from '../context/auth-context.js'
import WalletPage from './WalletPage.jsx'

vi.mock('../api/wallet.js', async (load) => {
  const actual = await load()
  return { ...actual, deposit: vi.fn(), getWalletBalance: vi.fn() }
})

afterEach(cleanup)

function setup(auth = { clear: vi.fn() }) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/app/carteira']}>
        <Routes>
          <Route path="/app/carteira" element={<WalletPage />} />
          <Route path="/login" element={<p>Login destino</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  return auth
}

describe('WalletPage balance', () => {
  beforeEach(() => vi.clearAllMocks())

  it('indica carregamento e exibe o saldo oficial em BRL', async () => {
    let resolve
    getWalletBalance.mockReturnValue(new Promise((done) => { resolve = done }))
    setup()
    expect(screen.getByRole('status')).toHaveTextContent('Carregando saldo…')

    resolve({ balance: 10000 })
    expect(await screen.findByText('R$ 10.000,00')).toBeInTheDocument()
  })

  it('apresenta erro funcional e permite nova tentativa', async () => {
    getWalletBalance.mockRejectedValueOnce(new WalletApiError('Servidor indisponível.'))
      .mockResolvedValueOnce({ balance: 250.5 })
    setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor indisponível.')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('R$ 250,50')).toBeInTheDocument()
    expect(getWalletBalance).toHaveBeenCalledTimes(2)
  })

  it('limpa dados privados e direciona ao login quando a sessão é inválida', async () => {
    getWalletBalance.mockRejectedValue(new WalletApiError('Sessão inválida.', {}, 401))
    const auth = setup()

    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    await waitFor(() => expect(auth.clear).toHaveBeenCalledTimes(1))
    expect(screen.queryByText(/Saldo disponível/)).not.toBeInTheDocument()
  })

  it.each(['', 'abc', '0', '-10', '9,99'])('não envia entrada inválida: %s', async (value) => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByText(/Informe|positivo|mínimo/)).toBeInTheDocument()
    expect(deposit).not.toHaveBeenCalled()
  })

  it('formata os dígitos como centavos enquanto a pessoa digita', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    setup()
    await screen.findByText('R$ 10.000,00')
    const input = screen.getByLabelText(/Valor do aporte/)

    fireEvent.change(input, { target: { value: '1' } })
    expect(input).toHaveValue('R$ 0,01')
    fireEvent.change(input, { target: { value: '12' } })
    expect(input).toHaveValue('R$ 0,12')
    fireEvent.change(input, { target: { value: '123' } })
    expect(input).toHaveValue('R$ 1,23')
  })

  it('cancela a confirmação sem enviar e preserva o valor informado', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    setup()
    await screen.findByText('R$ 10.000,00')
    const input = screen.getByLabelText(/Valor do aporte/)
    fireEvent.change(input, { target: { value: '50000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('R$ 500,00')
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(deposit).not.toHaveBeenCalled()
    expect(input).toHaveValue('R$ 500,00')
  })

  it('mantém o foco dentro da confirmação e o restaura ao fechar com Escape', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value: '10000' } })
    const continueButton = screen.getByRole('button', { name: 'Continuar' })
    continueButton.focus()
    fireEvent.click(continueButton)

    const cancelButton = screen.getByRole('button', { name: 'Cancelar' })
    const confirmButton = screen.getByRole('button', { name: 'Confirmar aporte' })
    await waitFor(() => expect(cancelButton).toHaveFocus())
    fireEvent.keyDown(cancelButton, { key: 'Tab', shiftKey: true })
    expect(confirmButton).toHaveFocus()
    fireEvent.keyDown(confirmButton, { key: 'Tab' })
    expect(cancelButton).toHaveFocus()
    fireEvent.keyDown(cancelButton, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(continueButton).toHaveFocus()
  })

  it('envia exatamente um aporte somente após a confirmação', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    deposit.mockResolvedValue({ balance: 10500 })
    setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value: '50000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(deposit).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar aporte' }))
    await waitFor(() => expect(deposit).toHaveBeenCalledTimes(1))
    expect(deposit).toHaveBeenCalledWith('500.00')
  })

  it('impede aportes duplicados enquanto o envio está pendente', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    deposit.mockReturnValue(new Promise(() => {}))
    setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value: '10000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    const confirm = screen.getByRole('button', { name: 'Confirmar aporte' })
    fireEvent.click(confirm)
    fireEvent.click(confirm)
    expect(deposit).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Enviando aporte…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  })

  it('mantém o contexto e permite tentar novamente após erro', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    deposit.mockRejectedValueOnce(new WalletApiError('Aporte temporariamente indisponível.'))
      .mockResolvedValueOnce({ balance: 10100 })
    setup()
    await screen.findByText('R$ 10.000,00')
    const input = screen.getByLabelText(/Valor do aporte/)
    fireEvent.change(input, { target: { value: '10000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar aporte' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Aporte temporariamente indisponível.')
    expect(input).toHaveValue('R$ 100,00')
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar aporte' }))
    expect(await screen.findByText('R$ 10.100,00')).toBeInTheDocument()
    expect(deposit).toHaveBeenCalledTimes(2)
  })

  it('atualiza o saldo somente com o valor oficial retornado pelo backend', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    deposit.mockResolvedValue({ balance: 10042.37 })
    setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value: '50000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar aporte' }))

    expect(await screen.findByText('R$ 10.042,37')).toBeInTheDocument()
    expect(screen.queryByText('R$ 10.500,00')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Aporte realizado')
    expect(screen.getByLabelText(/Valor do aporte/)).toHaveValue('')
  })

  it('remove o saldo e direciona ao login se a sessão expirar durante o aporte', async () => {
    getWalletBalance.mockResolvedValue({ balance: 10000 })
    deposit.mockRejectedValue(new WalletApiError('Sessão inválida.', {}, 401))
    const auth = setup()
    await screen.findByText('R$ 10.000,00')
    fireEvent.change(screen.getByLabelText(/Valor do aporte/), { target: { value: '10000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar aporte' }))

    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(auth.clear).toHaveBeenCalledTimes(1)
    expect(screen.queryByText('R$ 10.000,00')).not.toBeInTheDocument()
  })
})

import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AccountApiError, checkReactivation, reactivateAccount } from '../api/accounts.js'
import ReactivationPage from './ReactivationPage.jsx'

vi.mock('../api/accounts.js', async (original) => ({
  ...await original(),
  checkReactivation: vi.fn(),
  reactivateAccount: vi.fn(),
}))

afterEach(() => { cleanup(); vi.clearAllMocks() })

function setup() {
  render(
    <MemoryRouter initialEntries={['/reativacao']}>
      <Routes>
        <Route path="/reativacao" element={<ReactivationPage />} />
        <Route path="/login" element={<div>Login destino</div>} />
        <Route path="/cadastro" element={<div>Cadastro destino</div>} />
        <Route path="/app" element={<div>Área privada</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function fillCpf(value = '52998224725') {
  fireEvent.change(screen.getByLabelText(/CPF/), { target: { value } })
}

async function showOptions() {
  checkReactivation.mockResolvedValue({ reactivationAvailable: true })
  setup()
  fillCpf()
  fireEvent.click(screen.getByRole('button', { name: 'Consultar possibilidade' }))
  await screen.findByRole('heading', { name: 'Escolha como continuar' })
}

describe('ReactivationPage', () => {
  it('é acessível diretamente e exige CPF antes da consulta', async () => {
    setup()
    expect(screen.getByRole('heading', { name: 'Reative sua conta.' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Consultar possibilidade' }))
    expect(await screen.findByText('CPF é obrigatório.')).toBeInTheDocument()
    expect(checkReactivation).not.toHaveBeenCalled()
  })

  it('consulta o CPF formatado e apresenta as duas alternativas com o risco correto', async () => {
    await showOptions()
    expect(checkReactivation).toHaveBeenCalledWith('529.982.247-25')
    expect(screen.getByRole('button', { name: 'Reativar conta' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Criar nova conta' })).toBeEnabled()
    expect(screen.getByText(/A nova conta será independente/)).toBeInTheDocument()
    expect(screen.getByText(/não solicita comprovação adicional de identidade/)).toBeInTheDocument()
  })

  it('apresenta estado indisponível e permite consultar outro CPF', async () => {
    checkReactivation.mockResolvedValue({ reactivationAvailable: false })
    setup()
    fillCpf()
    fireEvent.click(screen.getByRole('button', { name: 'Consultar possibilidade' }))
    expect(await screen.findByRole('heading', { name: 'Reativação indisponível' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reativar conta' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Consultar outro CPF' }))
    expect(screen.getByLabelText(/CPF/)).toHaveValue('529.982.247-25')
  })

  it('preserva o CPF e apresenta erro funcional por campo', async () => {
    checkReactivation.mockRejectedValue(new AccountApiError('CPF inválido.', { cpf: ['Informe um CPF válido.'] }, 400))
    setup()
    fillCpf('123')
    fireEvent.click(screen.getByRole('button', { name: 'Consultar possibilidade' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('CPF inválido.')
    expect(screen.getByText('Informe um CPF válido.')).toBeInTheDocument()
    expect(screen.getByLabelText(/CPF/)).toHaveValue('123')
  })

  it('oferece nova tentativa após erro técnico seguro', async () => {
    checkReactivation.mockRejectedValueOnce(new AccountApiError('Servidor indisponível.', {}, 503)).mockResolvedValueOnce({ reactivationAvailable: true })
    setup()
    fillCpf()
    fireEvent.click(screen.getByRole('button', { name: 'Consultar possibilidade' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor indisponível.')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('heading', { name: 'Escolha como continuar' })).toBeInTheDocument()
    expect(checkReactivation).toHaveBeenCalledTimes(2)
  })

  it('reativa e direciona ao login sem acessar a área privada', async () => {
    reactivateAccount.mockResolvedValue()
    await showOptions()
    fireEvent.click(screen.getByRole('button', { name: 'Reativar conta' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(reactivateAccount).toHaveBeenCalledWith('529.982.247-25')
    expect(screen.queryByText('Área privada')).not.toBeInTheDocument()
  })

  it('preserva o fluxo consultado quando a reativação falha', async () => {
    reactivateAccount.mockRejectedValue(new AccountApiError('A conta não pode ser reativada.', {}, 409))
    await showOptions()
    fireEvent.click(screen.getByRole('button', { name: 'Reativar conta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('A conta não pode ser reativada.')
    expect(screen.getByRole('button', { name: 'Reativar conta' })).toBeEnabled()
    fireEvent.click(screen.getByRole('button', { name: 'Consultar outro CPF' }))
    expect(screen.getByLabelText(/CPF/)).toHaveValue('529.982.247-25')
  })

  it('leva à rota de cadastro sem duplicar o formulário', async () => {
    await showOptions()
    expect(screen.getByText(/a conta anterior permanecerá inacessível/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Criar nova conta' }))
    expect(await screen.findByText('Cadastro destino')).toBeInTheDocument()
  })

  it('impede consultas duplicadas enquanto a primeira está pendente', () => {
    checkReactivation.mockReturnValue(new Promise(() => {}))
    setup()
    fillCpf()
    const button = screen.getByRole('button', { name: 'Consultar possibilidade' })
    fireEvent.click(button)
    fireEvent.click(button)
    expect(checkReactivation).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Consultando…' })).toBeDisabled()
  })

  it('impede reativações duplicadas enquanto a primeira está pendente', async () => {
    reactivateAccount.mockReturnValue(new Promise(() => {}))
    await showOptions()
    const button = screen.getByRole('button', { name: 'Reativar conta' })
    fireEvent.click(button)
    fireEvent.click(button)
    expect(reactivateAccount).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Reativando…' })).toBeDisabled()
  })
})

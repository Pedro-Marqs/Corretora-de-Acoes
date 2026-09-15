import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { getWalletPositions, transferPosition, WalletApiError } from '../api/wallet.js'
import { AuthContext } from '../context/auth-context.js'
import TransfersPage from './TransfersPage.jsx'

vi.mock('../api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('../api/wallet.js', async (load) => ({ ...await load(), getWalletPositions: vi.fn(), transferPosition: vi.fn() }))

const brokers = [
  { associationId: 'origin', tradeName: 'Origem' },
  { associationId: 'destination', tradeName: 'Destino' },
]
const position = { assetId: 'asset', brokerageId: 'origin', brokerageName: 'Origem', ticker: 'PETR4', name: 'Petrobras', market: 'BR', quantity: 10, averagePriceBrl: 20 }
const snapshot = { availableBalance: 1000, positions: [position] }

afterEach(cleanup)

function setup(auth = { clear: vi.fn() }) {
  render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/app/transferencias']}><Routes><Route path="/app/transferencias" element={<TransfersPage />} /><Route path="/login" element={<p>Login destino</p>} /></Routes></MemoryRouter></AuthContext.Provider>)
  return auth
}

async function ready(customSnapshot = snapshot, customBrokers = brokers) {
  getWalletPositions.mockResolvedValue(customSnapshot)
  getActiveBrokers.mockResolvedValue(customBrokers)
  setup()
  await screen.findByRole('heading', { name: 'Transferir posição' })
}

function selectTransfer(quantity = '4') {
  fireEvent.change(screen.getByLabelText('Posição aberta'), { target: { value: 'asset:origin' } })
  fireEvent.change(screen.getByLabelText('Corretora de destino'), { target: { value: 'destination' } })
  fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: quantity } })
}

describe('TransfersPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('indica carregamento e apresenta a lista autenticada', async () => {
    let resolvePositions
    getWalletPositions.mockReturnValue(new Promise((resolve) => { resolvePositions = resolve }))
    getActiveBrokers.mockResolvedValue(brokers)
    setup()
    expect(screen.getByRole('status')).toHaveTextContent('Carregando posições e corretoras')
    resolvePositions(snapshot)
    expect(await screen.findByText(/Petrobras/)).toBeInTheDocument()
    expect(screen.getByText('R$ 1.000,00')).toBeInTheDocument()
  })

  it('apresenta o estado vazio sem formulário', async () => {
    await ready({ availableBalance: 1000, positions: [] })
    expect(screen.getByRole('heading', { name: 'Nenhuma posição aberta' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Continuar' })).not.toBeInTheDocument()
  })

  it('permite tentar novamente após falha de leitura', async () => {
    getWalletPositions.mockRejectedValueOnce(new WalletApiError('Carteira indisponível.')).mockResolvedValueOnce(snapshot)
    getActiveBrokers.mockResolvedValue(brokers)
    setup()
    expect(await screen.findByRole('alert')).toHaveTextContent('Carteira indisponível.')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText(/Petrobras/)).toBeInTheDocument()
    expect(getWalletPositions).toHaveBeenCalledTimes(2)
  })

  it('limpa dados privados e navega ao login em 401 na leitura', async () => {
    getWalletPositions.mockRejectedValue(new WalletApiError('Sessão expirada.', {}, 401))
    getActiveBrokers.mockResolvedValue(brokers)
    const auth = setup()
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(auth.clear).toHaveBeenCalledOnce()
    expect(screen.queryByText('PETR4')).not.toBeInTheDocument()
  })

  it('deriva a origem e exclui a corretora de origem dos destinos', async () => {
    await ready()
    fireEvent.change(screen.getByLabelText('Posição aberta'), { target: { value: 'asset:origin' } })
    expect(screen.getByLabelText('Corretora de origem')).toHaveValue('Origem')
    const destination = screen.getByLabelText('Corretora de destino')
    expect(within(destination).queryByRole('option', { name: 'Origem' })).not.toBeInTheDocument()
    expect(within(destination).getByRole('option', { name: 'Destino' })).toBeInTheDocument()
  })

  it.each(['', '0', '-1', '1.5', '11'])('rejeita quantidade inválida sem chamar a API: %s', async (quantity) => {
    await ready()
    selectTransfer(quantity)
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByText(/quantidade inteira positiva|máxima disponível/)).toBeInTheDocument()
    expect(transferPosition).not.toHaveBeenCalled()
  })

  it('orienta quando não existe destino distinto', async () => {
    await ready(snapshot, [brokers[0]])
    fireEvent.change(screen.getByLabelText('Posição aberta'), { target: { value: 'asset:origin' } })
    fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByText('Associe outra corretora ativa para transferir.')).toBeInTheDocument()
    expect(transferPosition).not.toHaveBeenCalled()
  })

  it('cancela preservando a seleção e restaura o foco', async () => {
    await ready(); selectTransfer()
    const trigger = screen.getByRole('button', { name: 'Continuar' })
    trigger.focus(); fireEvent.click(trigger)
    const confirm = screen.getByRole('button', { name: 'Confirmar transferência' })
    await waitFor(() => expect(confirm).toHaveFocus())
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(transferPosition).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Quantidade')).toHaveValue('4')
    await waitFor(() => expect(trigger).toHaveFocus())
  })

  it('fecha com Escape e mantém o foco preso na confirmação', async () => {
    await ready(); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    const cancel = screen.getByRole('button', { name: 'Cancelar' })
    const confirm = screen.getByRole('button', { name: 'Confirmar transferência' })
    await waitFor(() => expect(confirm).toHaveFocus())
    fireEvent.keyDown(confirm, { key: 'Tab' }); expect(cancel).toHaveFocus()
    fireEvent.keyDown(cancel, { key: 'Tab', shiftKey: true }); expect(confirm).toHaveFocus()
    fireEvent.keyDown(confirm, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('envia apenas uma solicitação enquanto a confirmação está pendente', async () => {
    getWalletPositions.mockResolvedValue(snapshot); getActiveBrokers.mockResolvedValue(brokers); transferPosition.mockReturnValue(new Promise(() => {})); setup()
    await screen.findByRole('heading', { name: 'Transferir posição' }); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    const confirm = screen.getByRole('button', { name: 'Confirmar transferência' })
    fireEvent.click(confirm); fireEvent.click(confirm)
    expect(transferPosition).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: 'Transferindo…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  })

  it('preserva o contexto e a mensagem funcional para nova tentativa', async () => {
    transferPosition.mockRejectedValue(new WalletApiError('Quantidade solicitada: 12; disponível: 10.', {}, 422))
    await ready(); selectTransfer('10'); fireEvent.click(screen.getByRole('button', { name: 'Continuar' })); fireEvent.click(screen.getByRole('button', { name: 'Confirmar transferência' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('solicitada: 12; disponível: 10')
    expect(screen.getByLabelText('Quantidade')).toHaveValue('10')
  })

  it('recarrega posições oficiais, conserva o saldo e só então declara sucesso', async () => {
    const refreshed = { availableBalance: 1000, positions: [{ ...position, quantity: 6 }, { ...position, brokerageId: 'destination', brokerageName: 'Destino', quantity: 4 }] }
    getWalletPositions.mockResolvedValueOnce(snapshot).mockResolvedValueOnce(refreshed)
    getActiveBrokers.mockResolvedValue(brokers); transferPosition.mockResolvedValue({}); setup()
    await screen.findByRole('heading', { name: 'Transferir posição' }); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' })); fireEvent.click(screen.getByRole('button', { name: 'Confirmar transferência' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Transferência concluída')
    expect(getWalletPositions).toHaveBeenCalledTimes(2)
    expect(screen.getByText('R$ 1.000,00')).toBeInTheDocument()
    expect(screen.getAllByText(/6 ações|4 ações/)).toHaveLength(4)
  })

  it('não declara sucesso local se a releitura oficial falhar', async () => {
    getWalletPositions.mockResolvedValueOnce(snapshot).mockRejectedValueOnce(new WalletApiError('Falha ao atualizar posições.'))
    getActiveBrokers.mockResolvedValue(brokers); transferPosition.mockResolvedValue({}); setup()
    await screen.findByRole('heading', { name: 'Transferir posição' }); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' })); fireEvent.click(screen.getByRole('button', { name: 'Confirmar transferência' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao atualizar posições.')
    expect(screen.queryByText('Transferência concluída')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Quantidade')).toHaveValue('4')
  })

  it('limpa dados privados em 401 durante o envio', async () => {
    transferPosition.mockRejectedValue(new WalletApiError('Sessão expirada.', {}, 401))
    await ready(); const auth = { clear: vi.fn() }
    cleanup(); getWalletPositions.mockResolvedValue(snapshot); getActiveBrokers.mockResolvedValue(brokers); setup(auth)
    await screen.findByRole('heading', { name: 'Transferir posição' }); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' })); fireEvent.click(screen.getByRole('button', { name: 'Confirmar transferência' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument(); expect(auth.clear).toHaveBeenCalledOnce(); expect(screen.queryByText('PETR4')).not.toBeInTheDocument()
  })

  it.each([320, 768, 1280])('mantém estrutura responsiva e acessível em %d px', async (width) => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: width })
    await ready(); selectTransfer(); fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(document.querySelector('.transfers-page')).toBeInTheDocument()
    expect(document.querySelector('.transfer-card')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Confirmar transferência' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})

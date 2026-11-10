import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { searchAsset } from '../api/market.js'
import { getWalletPositions, purchaseAsset, sellAsset, WalletApiError } from '../api/wallet.js'
import { AuthContext } from '../context/auth-context.js'
import OperationsPage from './OperationsPage.jsx'

vi.mock('../api/brokers.js', () => ({ getActiveBrokers: vi.fn() }))
vi.mock('../api/market.js', () => ({ searchAsset: vi.fn() }))
vi.mock('../api/wallet.js', () => ({ getWalletPositions: vi.fn(), purchaseAsset: vi.fn(), sellAsset: vi.fn(), WalletApiError: class WalletApiError extends Error { constructor(message, fieldErrors = {}, status) { super(message); this.fieldErrors = fieldErrors; this.status = status } } }))
afterEach(cleanup)

const broker = { associationId: 'broker-1', tradeName: 'Corretora Um' }
const position = { assetId: 'asset-1', brokerageId: 'broker-1', ticker: 'PETR4', name: 'Petrobras PN', market: 'BR', currency: 'BRL', brokerageName: 'Corretora Um', quantity: 10, averagePriceBrl: 20, quotePrice: 30, quotePriceBrl: 30, marketValueBrl: 300, unrealizedResultBrl: 100, usdBrlRate: null }
const asset = { assetId: 'asset-2', ticker: 'VALE3', name: 'Vale ON', market: 'BR', currency: 'BRL', originalPrice: 65, priceBrl: 65, quotePrice: 65, quotePriceBrl: 65, quoteStale: false, quoteQuotedAt: '2026-01-01T12:00:00Z', exchangeRateStale: false }
function setup({ positions = [position], brokers = [broker], auth = { clear: vi.fn() } } = {}) {
  getWalletPositions.mockResolvedValue({ availableBalance: 1000, positions })
  getActiveBrokers.mockResolvedValue(brokers)
  return render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/app/negociar']}><Routes><Route path="/app/negociar" element={<OperationsPage />} /><Route path="/login" element={<p>Login destino</p>} /></Routes></MemoryRouter></AuthContext.Provider>)
}

describe('OperationsPage', () => {
  beforeEach(() => { vi.clearAllMocks(); searchAsset.mockResolvedValue(asset); purchaseAsset.mockResolvedValue({}); sellAsset.mockResolvedValue({}) })

  it('carrega posições, pesquisa ativo e mostra carteira vazia', async () => {
    setup()
    expect(await screen.findByText('PETR4')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Informe um ticker.')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    expect(await screen.findByRole('dialog')).toHaveTextContent('VALE3')
    cleanup()
    setup({ positions: [] })
    expect(await screen.findByText('Sua carteira ainda está vazia')).toBeInTheDocument()
  })

  it('valida compra, confirma e recarrega a posição após sucesso', async () => {
    setup({ positions: [] })
    await screen.findByText('Sua carteira ainda está vazia')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    await screen.findByRole('dialog')
    fireEvent.click(screen.getByRole('button', { name: 'Comprar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('quantidade inteira positiva')
    fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Data e hora da operação'), { target: { value: '2026-01-01T10:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Comprar' }))
    expect(await screen.findByText('Confirmar compra')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar operação' }))
    await waitFor(() => expect(purchaseAsset).toHaveBeenCalledWith('asset-2', 'broker-1', 2, '65', expect.stringContaining('2026-01-01T10:00')))
    await waitFor(() => expect(getWalletPositions).toHaveBeenCalledTimes(2))
  })

  it('cancela venda e mostra erro de saldo sem reenviar', async () => {
    sellAsset.mockRejectedValueOnce(new WalletApiError('Saldo insuficiente.', {}, 409))
    setup()
    await screen.findByText('PETR4')
    fireEvent.click(screen.getByRole('button', { name: /PETR4/ }))
    fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Vender' }))
    expect(await screen.findByText('Confirmar venda')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(sellAsset).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Vender' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar operação' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Saldo insuficiente.')
    expect(sellAsset).toHaveBeenCalledTimes(1)
  })

  it('trata ativo inexistente, erro de cotação e sessão expirada', async () => {
    searchAsset.mockResolvedValueOnce(null).mockRejectedValueOnce(new Error('Cotação indisponível'))
    const auth = { clear: vi.fn() }
    setup({ positions: [], auth })
    await screen.findByText('Sua carteira ainda está vazia')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'XXXX3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nenhum ativo foi encontrado')
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Cotação indisponível')
    getWalletPositions.mockRejectedValueOnce(new WalletApiError('Sessão inválida.', {}, 401))
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('Login destino')).toBeInTheDocument()
    expect(auth.clear).toHaveBeenCalled()
  })

  it('rejeita operação com corretora, preço e data inválidos', async () => {
    setup({ positions: [], brokers: [] })
    await screen.findByText('Sua carteira ainda está vazia')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))
    await screen.findByRole('dialog')
    fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Comprar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('corretora ativa')
  })
})

import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { getCurrentAccount } from './api/accounts.js'
import { getActiveBrokers } from './api/brokers.js'
import { searchAsset } from './api/market.js'
import { getWalletPositions } from './api/wallet.js'

vi.mock('./api/accounts.js', async (load) => ({ ...await load(), getCurrentAccount: vi.fn() }))
vi.mock('./api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('./api/market.js', async (load) => ({ ...await load(), searchAsset: vi.fn() }))
vi.mock('./api/wallet.js', async (load) => ({ ...await load(), getWalletPositions: vi.fn() }))
afterEach(cleanup)

const asset = { assetId: 'asset-1', ticker: 'PETR4', name: 'Petrobras', market: 'BR', currency: 'BRL', priceBrl: '30.00' }
const brokers = [
  { associationId: 'broker-1', tradeName: 'Corretora Um' },
  { associationId: 'broker-2', tradeName: 'Corretora Dois' },
]
const positions = [
  { ...asset, brokerageId: 'broker-1', brokerageName: 'Corretora Um', quantity: 10, averagePriceBrl: '20.00', unrealizedResultBrl: '100.00', quotePriceBrl: '30.00' },
  { ...asset, brokerageId: 'broker-2', brokerageName: 'Corretora Dois', quantity: 25, averagePriceBrl: '24.00', unrealizedResultBrl: '150.00', quotePriceBrl: '30.00' },
]

describe('operations private route', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/app/operacoes')
    getActiveBrokers.mockResolvedValue(brokers)
    getWalletPositions.mockResolvedValue({ availableBalance: '1000.00', positions })
  })
  it('integra a página à rota e navegação privadas', async () => { getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' }); render(<App />); expect(await screen.findByRole('heading', { name: 'Compra e venda' })).toBeInTheDocument(); expect(screen.getByRole('link', { name: 'Comprar e vender' })).toHaveClass('active'); expect(getActiveBrokers).toHaveBeenCalledOnce() })
  it('não consulta nem exibe operações sem sessão', async () => { getCurrentAccount.mockRejectedValue({ status: 401 }); render(<App />); expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument(); expect(screen.queryByRole('heading', { name: 'Compra e venda' })).not.toBeInTheDocument(); expect(getActiveBrokers).not.toHaveBeenCalled() })

  it('troca os dados da posição conforme a corretora selecionada no modal', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /PETR4.*Corretora Um/i }))
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Corretora da posição').nextElementSibling).toHaveTextContent('Corretora Um')
    expect(within(dialog).getByText('Quantidade da posição').nextElementSibling).toHaveTextContent('10')

    fireEvent.change(within(dialog).getByLabelText('Corretora'), { target: { value: 'broker-2' } })

    expect(within(dialog).getByText('Corretora da posição').nextElementSibling).toHaveTextContent('Corretora Dois')
    expect(within(dialog).getByText('Quantidade da posição').nextElementSibling).toHaveTextContent('25')
  })

  it('usa a posição do snapshot do mesmo ativo e corretora ao buscar por ticker', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    searchAsset.mockResolvedValue(asset)
    render(<App />)
    await screen.findByRole('button', { name: /PETR4.*Corretora Um/i })
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'PETR4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByLabelText('Corretora')).toHaveValue('broker-1')
    expect(within(dialog).getByText('Corretora da posição').nextElementSibling).toHaveTextContent('Corretora Um')
    expect(within(dialog).getByText('Quantidade da posição').nextElementSibling).toHaveTextContent('10')
  })

  it('omite todo o bloco de posição para ativo ausente e mantém o saldo da carteira para compra', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    searchAsset.mockResolvedValue({ ...asset, assetId: 'asset-2', ticker: 'VALE3', name: 'Vale' })
    render(<App />)
    await screen.findByRole('button', { name: /PETR4.*Corretora Um/i })
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).queryByRole('definition', { name: 'Posição atual' })).not.toBeInTheDocument()
    expect(within(dialog).queryByText('Corretora da posição')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('Quantidade da posição')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('Preço médio acumulado')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('Lucro / perda')).not.toBeInTheDocument()
    expect(within(dialog).queryByText(/Sem posição|Indisponível/, { selector: 'dd' })).not.toBeInTheDocument()
    expect(within(dialog).getByText('Saldo disponível').nextElementSibling).toHaveTextContent('R$ 1.000,00')
    expect(within(dialog).getByText('Limite informativo para compra')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Comprar' })).toBeEnabled()
  })

  it('não inventa saldo quando o snapshot autenticado falha', async () => {
    getCurrentAccount.mockResolvedValue({ name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' })
    getWalletPositions.mockRejectedValue({ status: 503, message: 'Carteira temporariamente indisponível.' })
    searchAsset.mockResolvedValue({ ...asset, assetId: 'asset-2', ticker: 'VALE3', name: 'Vale' })
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Carteira temporariamente indisponível.')
    fireEvent.change(screen.getByLabelText('Ticker'), { target: { value: 'VALE3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar ativo' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Saldo disponível').nextElementSibling).toHaveTextContent('Indisponível')
    expect(within(dialog).queryByText('Quantidade da posição')).not.toBeInTheDocument()
  })
})

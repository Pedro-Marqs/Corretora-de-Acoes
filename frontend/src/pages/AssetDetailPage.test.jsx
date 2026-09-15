import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActiveBrokers } from '../api/brokers.js'
import { searchAsset } from '../api/market.js'
import { getWalletPositions } from '../api/wallet.js'
import { AuthContext } from '../context/auth-context.js'
import AssetDetailPage from './AssetDetailPage.jsx'
import OperationsPage from './OperationsPage.jsx'

vi.mock('../api/brokers.js', async (load) => ({ ...await load(), getActiveBrokers: vi.fn() }))
vi.mock('../api/market.js', async (load) => ({ ...await load(), searchAsset: vi.fn() }))
vi.mock('../api/wallet.js', async (load) => ({ ...await load(), getWalletPositions: vi.fn(), purchaseAsset: vi.fn(), sellAsset: vi.fn() }))
afterEach(cleanup)

const asset = { assetId: 'asset-1', ticker: 'PETR4', name: 'Petrobras', market: 'BR', currency: 'BRL', originalPrice: 35.5, priceBrl: 35.5, quoteQuotedAt: '2026-09-08T13:00:00Z', quoteStale: false, exchangeRateStale: false }

describe('AssetDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    searchAsset.mockResolvedValue(asset)
    getActiveBrokers.mockResolvedValue([{ associationId: 'broker-1', tradeName: 'Corretora Um' }])
    getWalletPositions.mockResolvedValue({ availableBalance: 1000, positions: [] })
  })

  it('preserva o ativo no fluxo Detalhe → Negociar → Operations', async () => {
    render(<AuthContext.Provider value={{ clear: vi.fn() }}><MemoryRouter initialEntries={['/app/bolsa/BR/PETR4']}><Routes><Route path="/app/bolsa/:market/:ticker" element={<AssetDetailPage />} /><Route path="/app/negociar" element={<OperationsPage />} /></Routes></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByRole('heading', { name: 'PETR4' })).toBeInTheDocument()
    expect(searchAsset).toHaveBeenCalledWith('PETR4', 'BR')
    fireEvent.click(screen.getByRole('button', { name: 'Negociar' }))

    expect(await screen.findByRole('heading', { name: 'Compra e venda' })).toBeInTheDocument()
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: /PETR4.*Petrobras/ })).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Quantidade')).toBeInTheDocument()
    expect(within(dialog).getByText('Preço oficial unitário').nextElementSibling).toHaveTextContent('R$ 35,50')
    expect(within(dialog).queryByRole('textbox', { name: /preço/i })).not.toBeInTheDocument()
  })
})

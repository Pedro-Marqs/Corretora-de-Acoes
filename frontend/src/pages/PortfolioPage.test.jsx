import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../context/auth-context.js'
import { getWalletPositions } from '../api/wallet.js'
import { exportPortfolioXlsx } from '../utils/portfolio-export.js'
import PortfolioPage from './PortfolioPage.jsx'

vi.mock('../api/wallet.js', async (load) => ({ ...await load(), getWalletPositions: vi.fn() }))
vi.mock('../utils/portfolio-export.js', () => ({ exportPortfolioXlsx: vi.fn() }))
afterEach(cleanup)

describe('PortfolioPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getWalletPositions.mockResolvedValue({ availableBalance: '5000.00', positions: [{ assetId: 'asset-1', ticker: 'PETR4', name: 'Petrobras PN', market: 'BR', brokerageId: 'broker-1', brokerageName: 'Corretora Um', quantity: 10, averagePriceBrl: '20.00', quotePriceBrl: '30.00', marketValueBrl: '300.00', unrealizedResultBrl: '100.00' }] })
  })

  it('exibe os ativos sem a busca de negociação e permite exportar', async () => {
    render(<AuthContext.Provider value={{ clear: vi.fn() }}><MemoryRouter><PortfolioPage /></MemoryRouter></AuthContext.Provider>)

    expect(await screen.findByRole('heading', { name: 'Carteira' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Ticker' })).toBeInTheDocument()
    expect(screen.getByText('PETR4')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Buscar ativo' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Exportar \.xlsx/ }))
    expect(exportPortfolioXlsx).toHaveBeenCalledWith(expect.arrayContaining([expect.objectContaining({ ticker: 'PETR4' })]))
  })
})

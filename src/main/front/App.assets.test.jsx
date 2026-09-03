import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { getCurrentAccount } from './api/accounts.js'
import { MarketApiError, searchAsset } from './api/market.js'

vi.mock('./api/accounts.js', async (load) => ({ ...await load(), getCurrentAccount: vi.fn() }))
vi.mock('./api/market.js', async (load) => ({ ...await load(), searchAsset: vi.fn() }))

const account = { name: 'Ana', cpf: '529.***.***-25', email: 'a***@example.com' }
const brAsset = {
  ticker: 'PETR4', name: 'Petrobras PN', market: 'BR', currency: 'BRL', originalPrice: '38.50',
  priceBrl: '38.50', quoteSource: 'Brapi', quoteQuotedAt: '2026-09-03T12:00:00Z', quoteStale: false,
  usdBrlRate: null, exchangeRateSource: null, exchangeRateQuotedAt: null, exchangeRateStale: null,
}
const usAsset = {
  ticker: 'AAPL', name: 'Apple Inc.', market: 'US', currency: 'USD', originalPrice: '225.10',
  priceBrl: '1238.05', quoteSource: 'Twelve Data', quoteQuotedAt: '2026-09-02T20:00:00Z', quoteStale: false,
  usdBrlRate: '5.50', exchangeRateSource: 'AwesomeAPI', exchangeRateQuotedAt: '2026-09-03T10:00:00Z', exchangeRateStale: false,
}

function submit(ticker) {
  fireEvent.change(screen.getByLabelText(/Ticker/), { target: { value: ticker } })
  fireEvent.click(screen.getByRole('button', { name: 'Pesquisar ativo' }))
}

describe('assets private route', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState({}, '', '/app/ativos')
    getCurrentAccount.mockResolvedValue(account)
  })
  afterEach(cleanup)

  it('integra rota e navegação privadas e inicia vazio, sem atualização manual', async () => {
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Pesquisa de ativos' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ativos' })).toHaveClass('active')
    expect(screen.getByText('Nenhuma pesquisa realizada')).toBeInTheDocument()
    expect(screen.getByLabelText(/Ticker/)).toBeRequired()
    expect(screen.queryByRole('button', { name: /atualizar/i })).not.toBeInTheDocument()
  })

  it('não renderiza nem consulta ativos sem sessão', async () => {
    getCurrentAccount.mockRejectedValue({ status: 401 })
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Pesquisa de ativos' })).not.toBeInTheDocument()
    expect(searchAsset).not.toHaveBeenCalled()
  })

  it('mostra resultado brasileiro e valores oficiais com duas casas', async () => {
    searchAsset.mockResolvedValue(brAsset)
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('petr4')
    expect(await screen.findByRole('heading', { name: 'PETR4' })).toBeInTheDocument()
    expect(screen.getByText('Petrobras PN')).toBeInTheDocument()
    expect(screen.getByText('Brasil')).toBeInTheDocument()
    expect(screen.getByText('BRL')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*38,50/)).toBeInTheDocument()
    expect(screen.getByText(/03\/09\/2026.*09:00/)).toBeInTheDocument()
    expect(searchAsset).toHaveBeenCalledWith('PETR4')
  })

  it('mostra resultado US, conversão e ambos os instantes recebidos', async () => {
    searchAsset.mockResolvedValue(usAsset)
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('AAPL')
    expect(await screen.findByRole('heading', { name: 'AAPL' })).toBeInTheDocument()
    expect(screen.getByText(/US\$\s*225,10/)).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*1\.238,05/)).toBeInTheDocument()
    expect(screen.getByText('5,50')).toBeInTheDocument()
    expect(screen.getByText(/02\/09\/2026.*17:00/)).toBeInTheDocument()
    expect(screen.getByText(/03\/09\/2026.*07:00/)).toBeInTheDocument()
  })

  it('bloqueia submissão duplicada durante o carregamento', async () => {
    searchAsset.mockReturnValue(new Promise(() => {}))
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('AAPL')
    const button = screen.getByRole('button', { name: 'Pesquisando…' })
    expect(button).toBeDisabled(); fireEvent.click(button)
    expect(screen.getByRole('status')).toHaveTextContent('Pesquisando AAPL')
    expect(searchAsset).toHaveBeenCalledTimes(1)
  })

  it('distingue resultado vazio de erro', async () => {
    searchAsset.mockResolvedValue(null)
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('VAZIO')
    expect(await screen.findByText('Ativo não encontrado')).toBeInTheDocument()
    expect(screen.getByText(/VAZIO/)).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.each([
    ['Ticker inválido.', 400], ['Mercado não suportado.', 422],
    ['A resposta do ativo está incompleta. Tente novamente mais tarde.', undefined],
    ['Não há cotação armazenada para este ativo.', 503],
    ['Não foi possível conectar ao servidor. Tente novamente em instantes.', undefined],
  ])('preserva ticker, mostra erro seguro e permite retry: %s', async (message, status) => {
    searchAsset.mockRejectedValueOnce(new MarketApiError(message, {}, status)).mockResolvedValueOnce(brAsset)
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('PETR4')
    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.getByLabelText(/Ticker/)).toHaveValue('PETR4')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('heading', { name: 'PETR4' })).toBeInTheDocument()
    expect(searchAsset).toHaveBeenCalledTimes(2)
  })

  it('remove dados e direciona ao login em resposta 401', async () => {
    searchAsset.mockRejectedValue(new MarketApiError('detalhe interno', {}, 401))
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('AAPL')
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta.' })).toBeInTheDocument()
    expect(screen.queryByText('detalhe interno')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Pesquisa de ativos' })).not.toBeInTheDocument()
  })

  it('renderiza avisos independentes pelas flags e preserva valores e horários', async () => {
    searchAsset.mockResolvedValue({ ...usAsset, quoteStale: true, exchangeRateStale: true })
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('AAPL')
    expect(await screen.findByText('Cotação desatualizada')).toBeInTheDocument()
    expect(screen.getByText('USD/BRL desatualizado')).toBeInTheDocument()
    expect(screen.getByText(/US\$\s*225,10/)).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*1\.238,05/)).toBeInTheDocument()
    expect(screen.getAllByText(/02\/09\/2026.*17:00/)).toHaveLength(2)
    expect(screen.getAllByText(/03\/09\/2026.*07:00/)).toHaveLength(2)
  })

  it('não exibe avisos quando o backend marca ambos os limites como atuais', async () => {
    searchAsset.mockResolvedValue(usAsset)
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' }); submit('AAPL')
    await screen.findByRole('heading', { name: 'AAPL' })
    expect(screen.queryByText(/desatualizad[oa]/i)).not.toBeInTheDocument()
  })

  it('mantém controles utilizáveis em viewport de 320 px', async () => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 320 })
    render(<App />); await screen.findByRole('heading', { name: 'Pesquisa de ativos' })
    expect(screen.getByLabelText(/Ticker/)).toBeVisible()
    expect(screen.getByRole('button', { name: 'Pesquisar ativo' })).toBeVisible()
  })
})

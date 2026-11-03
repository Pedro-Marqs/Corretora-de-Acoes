import { afterEach, describe, expect, it, vi } from 'vitest'
import { exportPortfolioXlsx } from './portfolio-export.js'

describe('exportPortfolioXlsx', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('gera um arquivo XLSX com as posições e agenda a revogação da URL', () => {
    vi.useFakeTimers()
    const createObjectURL = vi.fn(() => 'blob:portfolio')
    const revokeObjectURL = vi.fn()
    Object.defineProperty(window.URL, 'createObjectURL', {
      configurable: true,
      value: createObjectURL,
    })
    Object.defineProperty(window.URL, 'revokeObjectURL', {
      configurable: true,
      value: revokeObjectURL,
    })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    const exported = exportPortfolioXlsx([
      {
        ticker: 'PETR4',
        name: 'Petrobras <PN>',
        market: 'BR',
        brokerageName: 'Broker',
        quantity: 10,
        averagePriceBrl: '20.00',
        quotePriceBrl: '25.00',
        marketValueBrl: '250.00',
        unrealizedResultBrl: '50.00',
      },
      {
        ticker: 'AAPL',
        name: 'Apple',
        market: 'US',
        brokerageName: 'Broker',
        quantity: 2,
        averagePriceBrl: '200.00',
        quotePriceBrl: null,
        marketValueBrl: null,
        unrealizedResultBrl: null,
      },
    ], 'carteira-teste.xlsx')

    expect(exported).toBe(true)
    expect(createObjectURL).toHaveBeenCalledWith(expect.any(Blob))
    expect(click).toHaveBeenCalledOnce()
    expect(click.mock.instances[0].download).toBe('carteira-teste.xlsx')

    vi.runAllTimers()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:portfolio')
  })

  it('retorna falso quando a API de URL do navegador não está disponível', () => {
    vi.stubGlobal('document', undefined)

    expect(exportPortfolioXlsx([])).toBe(false)
  })
})

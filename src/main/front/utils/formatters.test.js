import { describe, expect, it } from 'vitest'
import { currencyInputToDecimal, formatBrasiliaDateTime, formatCurrency, formatCurrencyInput } from './formatters.js'
describe('formatters', () => {
  it.each([['1', 'R$ 0,01'], ['12', 'R$ 0,12'], ['123', 'R$ 1,23'], ['123456', 'R$ 1.234,56']])('formata entrada monetária da direita para a esquerda: %s', (value, expected) => {
    expect(formatCurrencyInput(value)).toBe(expected)
  })

  it('converte a entrada monetária para decimal aceito pela API', () => {
    expect(currencyInputToDecimal('R$ 1.234,56')).toBe('1234.56')
    expect(currencyInputToDecimal('')).toBe('')
  })
  it('formata BRL com duas casas', () => { expect(formatCurrency(1234.5)).toMatch(/R\$\s*1\.234,50/) })
  it('formata no horário de Brasília', () => { expect(formatBrasiliaDateTime('2026-08-17T15:00:00Z')).toContain('12:00') })
  it('usa fallback para entradas inválidas', () => { expect(formatCurrency('x')).toBe('Valor indisponível'); expect(formatBrasiliaDateTime('x')).toBe('Data indisponível') })
})

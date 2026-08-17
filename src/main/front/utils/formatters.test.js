import { describe, expect, it } from 'vitest'
import { formatBrasiliaDateTime, formatCurrency } from './formatters.js'
describe('formatters', () => {
  it('formata BRL com duas casas', () => { expect(formatCurrency(1234.5)).toMatch(/R\$\s*1\.234,50/) })
  it('formata no horário de Brasília', () => { expect(formatBrasiliaDateTime('2026-08-17T15:00:00Z')).toContain('12:00') })
  it('usa fallback para entradas inválidas', () => { expect(formatCurrency('x')).toBe('Valor indisponível'); expect(formatBrasiliaDateTime('x')).toBe('Data indisponível') })
})

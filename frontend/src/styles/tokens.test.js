import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cwd } from 'node:process'
import { describe, expect, it } from 'vitest'

const tokens = readFileSync(resolve(cwd(), 'src/styles/tokens.css'), 'utf8')

describe('tokens do design system', () => {
  it('centraliza a identidade escura e esmeralda', () => {
    expect(tokens).toContain('--color-canvas: #0b1210')
    expect(tokens).toContain('--color-surface-raised: #172721')
    expect(tokens).toContain('--color-primary: #20c997')
    expect(tokens).toContain('--color-text-secondary: #c0d2ca')
    expect(tokens).toContain('--color-danger-surface: #45282d')
    expect(tokens).toContain('--font-family-sans:')
  })

  it('documenta escala, responsividade e estados semânticos', () => {
    expect(tokens).toContain('--space-16: 4rem')
    expect(tokens).toContain('--radius-xl: 1.25rem')
    expect(tokens).toContain('--shadow-focus:')
    expect(tokens).toContain('--breakpoint-sm: 32rem')
    expect(tokens).toContain('--color-conflict:')
    expect(tokens).toContain('color-scheme: dark')
  })
})

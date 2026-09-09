import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cwd } from 'node:process'
import { describe, expect, it } from 'vitest'

const globalStyles = readFileSync(resolve(cwd(), 'styles/global.css'), 'utf8')

describe('tema global', () => {
  it('centraliza a direção azul usada nas rotas públicas e privadas', () => {
    expect(globalStyles).toContain('--blue-950:#071a2b')
    expect(globalStyles).toContain('--blue-600:#176fa3')
    expect(globalStyles).toMatch(/\.brand-panel[^}]*var\(--blue-950\)/)
    expect(globalStyles).toMatch(/\.auth-page[^}]*var\(--blue-50\)/)
    expect(globalStyles).toMatch(/\.private-layout[^}]*var\(--blue-50\)/)
    expect(globalStyles).toMatch(/\.primary-button[^}]*var\(--blue-600\)/)
    expect(globalStyles).toMatch(/\.operation-modal-card[^}]*#102b3c/)
  })

  it('preserva tokens semânticos, foco e breakpoints responsivos', () => {
    expect(globalStyles).toContain('--success:#087a62')
    expect(globalStyles).toContain('--warning:#8a5a13')
    expect(globalStyles).toContain('--danger:#a13737')
    expect(globalStyles).toContain('outline: 3px solid var(--focus)')
    expect(globalStyles).toContain('@media (max-width: 860px)')
    expect(globalStyles).toContain('@media (max-width: 420px)')
    expect(globalStyles).not.toMatch(/#(?:123f32|c9ea98|163c30|1b604b|e2f1ce|f4f6f1)\b/i)
  })
})

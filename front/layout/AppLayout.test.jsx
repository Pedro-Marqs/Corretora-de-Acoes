import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../context/auth-context.js'
import AppLayout from './AppLayout.jsx'

afterEach(cleanup)

function setup(path = '/app/investimentos') {
  render(<AuthContext.Provider value={{ account: { name: 'Ana Silva' }, clear: vi.fn() }}><MemoryRouter initialEntries={[path]}><Routes><Route path="/app" element={<AppLayout />}><Route path="*" element={<main><h1>Conteúdo privado</h1></main>} /></Route></Routes></MemoryRouter></AuthContext.Provider>)
}

describe('AppLayout', () => {
  it('mantém somente as três seções principais no header e expõe o menu da conta', () => {
    setup()
    const navigation = screen.getByRole('navigation', { name: 'Navegação principal' })
    expect(within(navigation).getAllByRole('link').map((link) => link.textContent)).toEqual(['Investimentos', 'Banco', 'Bolsa'])
    expect(within(navigation).queryByRole('link', { name: /Transferências|Histórico/ })).not.toBeInTheDocument()

    const trigger = screen.getByRole('button', { name: 'Ana Silva' })
    fireEvent.click(trigger)
    const menu = screen.getByRole('menu')
    expect(within(menu).getByRole('menuitem', { name: 'Configurações' })).toHaveAttribute('href', '/app/conta')
    expect(within(menu).getByRole('menuitem', { name: 'Sair' })).toBeInTheDocument()
    fireEvent.keyDown(menu, { key: 'Escape' })
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it.each([
    ['/app/historico', 'Investimentos'],
    ['/app/transferencias', 'Investimentos'],
    ['/app/corretoras', 'Banco'],
    ['/app/operacoes', 'Bolsa'],
  ])('indica %s no contexto %s', (path, section) => {
    setup(path)
    expect(screen.getByRole('link', { name: section })).toHaveClass('active')
  })
})

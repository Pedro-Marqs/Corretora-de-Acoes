import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { searchAsset } from '../api/market.js'
import { AuthContext } from '../context/auth-context.js'
import AppLayout from './AppLayout.jsx'

vi.mock('../api/market.js', async (load) => ({ ...await load(), searchAsset: vi.fn() }))
afterEach(cleanup)

function setup(path = '/app/banco') {
  render(<AuthContext.Provider value={{ account: { name: 'Ana Silva' }, clear: vi.fn() }}><MemoryRouter initialEntries={[path]}><Routes><Route path="/app" element={<AppLayout />}><Route path="*" element={<main><h1>Conteúdo privado</h1></main>} /></Route></Routes></MemoryRouter></AuthContext.Provider>)
}

describe('AppLayout', () => {
  it('expõe a barra lateral e o menu da conta', () => {
    setup()
    const navigation = screen.getByRole('navigation', { name: 'Navegação principal' })
    expect(within(navigation).getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(within(navigation).getByRole('link', { name: 'Banco' })).toHaveClass('active')
    expect(within(navigation).getByRole('link', { name: 'Carteira' })).toBeInTheDocument()
    expect(within(navigation).getByRole('link', { name: 'Ativos' })).toBeInTheDocument()
    expect(within(navigation).getByRole('link', { name: 'Transferências' })).toBeInTheDocument()
    expect(within(navigation).getByRole('link', { name: 'Corretoras' })).toBeInTheDocument()

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
    ['/app/historico', 'Histórico'],
    ['/app/transferencias', 'Transferências'],
    ['/app/corretoras', 'Corretoras'],
    ['/app/operacoes', 'Carteira'],
  ])('indica %s no contexto %s', (path, section) => {
    setup(path)
    expect(screen.getByRole('link', { name: section })).toHaveClass('active')
  })

  it('alterna menus e pesquisa primeiro no Brasil e depois nos Estados Unidos', async () => {
    searchAsset.mockResolvedValueOnce(null).mockResolvedValueOnce({ market: 'US', ticker: 'AAPL' })
    setup()
    fireEvent.click(screen.getByRole('button', { name: 'Recolher menu' }))
    expect(screen.getByRole('navigation', { name: /Navega/ }).parentElement).toHaveClass('is-collapsed')
    fireEvent.click(screen.getByRole('button', { name: 'Expandir menu' }))
    fireEvent.click(screen.getByRole('button', { name: 'Abrir menu' }))
    expect(screen.getByRole('button', { name: 'Fechar menu' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Fechar menu' }))
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar ativos' }), { target: { value: ' aapl! ' } })
    fireEvent.click(screen.getByRole('button', { name: /Pesquisar no cabe/ }))
    expect(await screen.findByText(/Conte/)).toBeInTheDocument()
    expect(searchAsset).toHaveBeenNthCalledWith(1, 'AAPL', 'BR')
    expect(searchAsset).toHaveBeenNthCalledWith(2, 'AAPL', 'US')
  })

  it('continua para a busca ampla quando nenhum mercado encontra o ativo', async () => {
    searchAsset.mockResolvedValue(null)
    setup()
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar ativos' }), { target: { value: 'PETR4' } })
    fireEvent.click(screen.getByRole('button', { name: /Pesquisar no cabe/ }))
    expect(await screen.findByText(/Conte/)).toBeInTheDocument()
    expect(searchAsset).toHaveBeenCalledTimes(2)
  })
})

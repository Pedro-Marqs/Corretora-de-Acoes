import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../context/auth-context.js'
import PublicRoute from './PublicRoute.jsx'
afterEach(cleanup)
function renderRoute(value) {
  return render(<AuthContext.Provider value={value}><MemoryRouter initialEntries={['/login']}><Routes><Route path="/login" element={<PublicRoute><div>Página pública</div></PublicRoute>} /><Route path="/app" element={<div>Área privada</div>} /></Routes></MemoryRouter></AuthContext.Provider>)
}
describe('PublicRoute', () => {
  it('hidrata sessão desconhecida e mostra loading', () => { const refresh=vi.fn(); renderRoute({ status:'unknown', refresh }); expect(screen.getByRole('status')).toHaveTextContent('Validando sua sessão'); expect(refresh).toHaveBeenCalled() })
  it('redireciona sessão válida', () => { renderRoute({ status:'authenticated', refresh:vi.fn() }); expect(screen.getByText('Área privada')).toBeInTheDocument() })
  it('permite rota pública após 401', () => { renderRoute({ status:'anonymous', refresh:vi.fn() }); expect(screen.getByText('Página pública')).toBeInTheDocument() })
  it('mostra erro recuperável em falha de rede', () => { const refresh=vi.fn(); renderRoute({ status:'error', error:'Sem conexão', refresh }); expect(screen.getByRole('alert')).toHaveTextContent('Sem conexão'); screen.getByRole('button').click(); expect(refresh).toHaveBeenCalled() })
})

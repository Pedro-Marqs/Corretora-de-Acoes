import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { EmptyState, ErrorState, LoadingState, Message } from './AsyncStates.jsx'
afterEach(cleanup)
describe('estados comuns', () => {
  it('renderiza carregamento e vazio', () => { render(<><LoadingState /><EmptyState title="Sem registros" /></>); expect(screen.getByRole('status')).toBeInTheDocument(); expect(screen.getByText('Sem registros')).toBeInTheDocument() })
  it('renderiza erro com nova tentativa', () => { const retry=vi.fn(); render(<ErrorState message="Falha segura" onRetry={retry} />); fireEvent.click(screen.getByRole('button')); expect(retry).toHaveBeenCalled() })
  it('renderiza mensagem acessível', () => { render(<Message kind="error">Algo falhou</Message>); expect(screen.getByRole('alert')).toHaveTextContent('Algo falhou') })
})

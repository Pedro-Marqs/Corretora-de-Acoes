import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  AsyncState,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  FilterBar,
  MetricCard,
  MutationFeedback,
  PageShell,
  Pagination,
  SectionHeader,
  SelectField,
  StatusMessage,
  SurfaceCard,
  TextField,
} from './index.jsx'

afterEach(cleanup)

describe('primitivas UI do design system', () => {
  it('compõe shell, cabeçalho, superfície, métrica e badge', () => {
    render(<PageShell><SectionHeader eyebrow="Resumo" title="Carteira" description="Dados oficiais" actions={<Button>Atualizar</Button>} /><SurfaceCard tone="raised"><MetricCard label="Patrimônio" value="R$ 100,00" detail="BRL" trend="+2%" trendTone="positive" icon="↗" /><Badge tone="success">Ativo</Badge></SurfaceCard></PageShell>)
    expect(screen.getByRole('heading', { name: 'Carteira' })).toBeInTheDocument()
    expect(screen.getByText('R$ 100,00')).toBeInTheDocument()
    expect(screen.getByText('Ativo')).toHaveClass('ui-badge--success')
    expect(screen.getByText('Atualizar')).toHaveClass('ui-button--primary')
  })

  it('padroniza botão, campos, filtros e erros acessíveis', () => {
    const submit = vi.fn()
    render(<><Button loading onClick={submit}>Salvar</Button><TextField label="Nome" id="name" hint="Nome completo" required /><TextField label="E-mail" id="email" error={['E-mail inválido']} /><SelectField label="Mercado" id="market" error="Selecione um mercado"><option value="">Selecione</option></SelectField><FilterBar onSubmit={submit}><TextField label="Busca" id="search" /></FilterBar></>)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled()
    expect(screen.getByLabelText(/Nome/)).toHaveAttribute('aria-describedby', 'name-hint')
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Mercado')).toHaveAttribute('aria-describedby', 'market-error')
    expect(screen.getByLabelText('Busca').closest('form')).toHaveClass('ui-filter-bar')
  })

  it('renderiza tabelas com colunas, célula customizada e estado vazio', () => {
    const columns = [{ key: 'name', label: 'Nome' }, { key: 'status', label: 'Status', render: (row) => <Badge>{row.status}</Badge> }]
    const { rerender } = render(<DataTable caption="Investidores" columns={columns} rows={[{ id: '1', name: 'Ana', status: 'Ativa' }]} rowKey={(row) => row.id} />)
    expect(screen.getByRole('table', { name: 'Investidores' })).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('Ativa')).toBeInTheDocument()
    rerender(<DataTable columns={columns} rows={[]} empty="Nada encontrado" />)
    expect(screen.getByText('Nada encontrado')).toBeInTheDocument()
  })

  it('mantém paginação acionável e informa a página atual', () => {
    const change = vi.fn()
    render(<Pagination page={1} totalPages={3} onPageChange={change} />)
    fireEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    fireEvent.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(change).toHaveBeenNthCalledWith(1, 0)
    expect(change).toHaveBeenNthCalledWith(2, 2)
    expect(screen.getByText('Página 2 de 3')).toBeInTheDocument()
  })

  it('expõe estados assíncronos, sucesso e conflito', () => {
    const retry = vi.fn()
    const { rerender } = render(<AsyncState status="loading" loadingMessage="Carregando dados" />)
    expect(screen.getByRole('status')).toHaveTextContent('Carregando dados')
    rerender(<AsyncState status="empty" emptyTitle="Sem posições" emptyDescription="Não há dados." />)
    expect(screen.getByText('Sem posições')).toBeInTheDocument()
    rerender(<AsyncState status="error" errorMessage="Falha segura" onRetry={retry} />)
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(retry).toHaveBeenCalledOnce()
    rerender(<StatusMessage kind="conflict" title="Conflito">A lista foi atualizada.</StatusMessage>)
    expect(screen.getByRole('alert')).toHaveTextContent('Conflito')
  })

  it('consolida feedback pós-mutação sem declarar sucesso localmente', () => {
    const retry = vi.fn()
    const { rerender } = render(<MutationFeedback status="pending" />)
    expect(screen.getByRole('status')).toHaveTextContent('resposta oficial')
    rerender(<MutationFeedback status="success" successMessage="Saldo atualizado" />)
    expect(screen.getByRole('status')).toHaveTextContent('Saldo atualizado')
    rerender(<MutationFeedback status="conflict" onRetry={retry} />)
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(retry).toHaveBeenCalledOnce()
    rerender(<MutationFeedback status="error" errorMessage="Falha segura" />)
    expect(screen.getByRole('alert')).toHaveTextContent('Falha segura')
    rerender(<MutationFeedback status="idle" />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('foca diálogo, fecha com Escape e devolve o foco ao acionador', () => {
    const close = vi.fn()
    const trigger = document.createElement('button')
    document.body.append(trigger)
    trigger.focus()
    render(<ConfirmDialog open title="Confirmar alteração" description="A ação será enviada ao servidor." onClose={close} onConfirm={vi.fn()} />)
    expect(screen.getByRole('dialog')).toHaveFocus()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(close).toHaveBeenCalledOnce()
    cleanup()
    expect(document.activeElement).toBe(trigger)
    trigger.remove()
  })

  it('mantém o foco contido no diálogo durante a navegação por teclado', () => {
    const trigger = document.createElement('button')
    document.body.append(trigger)
    trigger.focus()
    render(<ConfirmDialog open title="Confirmar alteração" onClose={vi.fn()} onConfirm={vi.fn()} />)
    const dialog = screen.getByRole('dialog')
    const close = screen.getByRole('button', { name: 'Fechar' })
    const confirm = screen.getByRole('button', { name: 'Confirmar' })
    close.focus()
    fireEvent.keyDown(dialog, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(confirm)
    fireEvent.keyDown(dialog, { key: 'Tab' })
    expect(document.activeElement).toBe(close)
    trigger.remove()
  })
})

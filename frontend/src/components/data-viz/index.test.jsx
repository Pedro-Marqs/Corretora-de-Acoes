import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { BarList, ResponsiveLineChart } from './index.jsx'

afterEach(cleanup)

describe('primitivas de visualização acessível', () => {
  it('renderiza pontos oficiais e uma alternativa tabular com unidade e período', () => {
    render(<ResponsiveLineChart label="Patrimônio oficial" description="Evolução patrimonial" period="4W" currency="BRL" data={[{ recordedAt: '01/01', patrimonyBrl: 100 }, { recordedAt: '02/01', patrimonyBrl: 125 }]} labelKey="recordedAt" valueKey="patrimonyBrl" formatValue={(value) => `R$ ${value}`} />)
    expect(screen.getByRole('img', { name: 'Patrimônio oficial' })).toBeInTheDocument()
    fireEvent.click(screen.getByText('Consultar pontos em tabela'))
    expect(screen.getByRole('table', { name: /Patrimônio oficial/ })).toHaveTextContent('R$ 125')
    expect(screen.getByRole('img', { name: 'Patrimônio oficial' })).toHaveAccessibleDescription(/Período: 4W\. Moeda: BRL/)
  })

  it('não inventa série quando não há valores oficiais utilizáveis', () => {
    render(<ResponsiveLineChart label="Patrimônio" data={[{ label: 'A', value: null }]} />)
    expect(screen.getByRole('status')).toHaveTextContent('Dados indisponíveis')
  })

  it('renderiza distribuição oficial em barras e estado vazio', () => {
    const { rerender } = render(<BarList label="Por mercado" data={[{ market: 'BR', valueBrl: 200 }, { market: 'US', valueBrl: 100 }]} labelKey="market" valueKey="valueBrl" formatValue={(value) => `R$ ${value}`} />)
    expect(screen.getByLabelText('Por mercado')).toBeInTheDocument()
    expect(screen.getByText('R$ 200')).toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
    rerender(<BarList data={[]} />)
    expect(screen.getByRole('status')).toHaveTextContent('Dados indisponíveis')
  })
})

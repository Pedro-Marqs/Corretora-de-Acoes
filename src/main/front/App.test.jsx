import '@testing-library/jest-dom/vitest'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.jsx'

describe('App', () => {
  it('renders the project title', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: 'Gestão de Ações e Corretoras' }),
    ).toBeInTheDocument()
  })
})

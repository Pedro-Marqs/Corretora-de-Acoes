import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../context/auth-context.js'
import PrivateRoute from './PrivateRoute.jsx'

afterEach(cleanup)

function renderRoute(value) {
  return render(<AuthContext.Provider value={value}><MemoryRouter initialEntries={['/app']}><Routes>
    <Route path="/app" element={<PrivateRoute><div>Private area</div></PrivateRoute>} />
    <Route path="/login" element={<div>Public login</div>} />
  </Routes></MemoryRouter></AuthContext.Provider>)
}

describe('PrivateRoute', () => {
  it('keeps loading while the provider restores the session', () => {
    const refresh = vi.fn()
    renderRoute({ status: 'loading', refresh })
    expect(screen.getByRole('status')).toHaveTextContent('Validando sua sess')
    expect(screen.queryByText('Public login')).not.toBeInTheDocument()
  })

  it('redirects only after the session is anonymous', () => {
    renderRoute({ status: 'anonymous', refresh: vi.fn() })
    expect(screen.getByText('Public login')).toBeInTheDocument()
  })

  it('renders the private area for an authenticated session', () => {
    renderRoute({ status: 'authenticated', refresh: vi.fn() })
    expect(screen.getByText('Private area')).toBeInTheDocument()
  })

  it('keeps a network error retryable', () => {
    const refresh = vi.fn()
    renderRoute({ status: 'error', error: 'Network unavailable', refresh })
    expect(screen.getByRole('alert')).toHaveTextContent('Network unavailable')
    screen.getByRole('button').click()
    expect(refresh).toHaveBeenCalledTimes(1)
  })
})

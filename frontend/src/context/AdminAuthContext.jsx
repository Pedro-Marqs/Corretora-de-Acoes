import { useCallback, useEffect, useRef, useState } from 'react'
import { endAdminSession, getAdminAccessToken, getAdminSessionGeneration } from '../api/admin-auth-token.js'
import { loginAdmin, logoutAdmin, restoreAdminSession } from '../api/admin-auth.js'
import { AdminAuthContext } from './admin-auth-context.js'

export function AdminAuthProvider({ children }) {
  const [state, setState] = useState({ status: 'unknown', error: '' })
  const requestId = useRef(0)

  const clear = useCallback(() => {
    requestId.current += 1
    endAdminSession()
    setState({ status: 'anonymous', error: '' })
  }, [])

  const restore = useCallback(async () => {
    const request = ++requestId.current
    const generation = getAdminSessionGeneration()
    setState((current) => ({ ...current, status: 'loading', error: '' }))
    try {
      await restoreAdminSession()
      if (request !== requestId.current || generation !== getAdminSessionGeneration()) return false
      setState({ status: 'authenticated', error: '' })
      return true
    } catch (error) {
      if (request !== requestId.current || generation !== getAdminSessionGeneration()) return false
      if (error.status === 401 || error.status === 403) {
        endAdminSession()
        setState({ status: 'anonymous', error: '' })
      } else setState({ status: 'error', error: error.message || 'Não foi possível validar a sessão administrativa.' })
      return false
    }
  }, [])

  const login = useCallback(async (credentials) => {
    const request = ++requestId.current
    setState({ status: 'loading', error: '' })
    try {
      await loginAdmin(credentials)
      if (request !== requestId.current) return false
      setState({ status: 'authenticated', error: '' })
      return true
    } catch (error) {
      if (request === requestId.current) setState({ status: 'anonymous', error: error.message || 'Não foi possível entrar.' })
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    requestId.current += 1
    setState({ status: 'anonymous', error: '' })
    await logoutAdmin()
  }, [])

  useEffect(() => {
    let mounted = true
    void Promise.resolve().then(() => { if (mounted) restore() })
    return () => { mounted = false; requestId.current += 1 }
  }, [restore])

  return <AdminAuthContext.Provider value={{ ...state, isAuthenticated: state.status === 'authenticated', accessToken: getAdminAccessToken(), login, logout, restore, clear }}>{children}</AdminAuthContext.Provider>
}

import { useCallback, useState } from 'react'
import { getCurrentAccount } from '../api/accounts.js'
import { AuthContext } from './auth-context.js'

export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'unknown', account: null, error: '' })
  const refresh = useCallback(async function refresh() {
    setState((current) => ({ ...current, status: 'loading', error: '' }))
    try { const account = await getCurrentAccount(); setState({ status: 'authenticated', account, error: '' }); return true }
    catch (error) {
      if (error.status === 401) { setState({ status: 'anonymous', account: null, error: '' }); return false }
      setState({ status: 'error', account: null, error: error.message }); return false
    }
  }, [])
  const clear = useCallback(function clear() { setState({ status: 'anonymous', account: null, error: '' }) }, [])
  return <AuthContext.Provider value={{ ...state, refresh, clear }}>{children}</AuthContext.Provider>
}

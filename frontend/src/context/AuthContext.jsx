import { useCallback, useEffect, useRef, useState } from 'react'
import { getCurrentAccount } from '../api/accounts.js'
import { restoreSession } from '../api/auth.js'
import { endSession, getAccessToken, getSessionGeneration } from '../api/auth-token.js'
import { AuthContext } from './auth-context.js'

export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'unknown', account: null, error: '' })
  const requestId = useRef(0)

  const refresh = useCallback(async function refresh() {
    const currentRequest = ++requestId.current
    const expectedGeneration = getSessionGeneration()
    setState((current) => ({ ...current, status: 'loading', error: '' }))

    const isCurrent = () => currentRequest === requestId.current
      && expectedGeneration === getSessionGeneration()

    try {
      if (!getAccessToken()) {
        try {
          await restoreSession()
        } catch (error) {
          // Keep an explicitly enabled legacy SESSION usable during migration.
          if (error.status !== 401 && error.status !== 403) throw error
        }
      }

      if (!isCurrent()) return false
      const account = await getCurrentAccount()
      if (!isCurrent()) return false
      setState({ status: 'authenticated', account, error: '' })
      return true
    } catch (error) {
      if (!isCurrent()) return false
      if (error.status === 401 || error.status === 403) {
        endSession()
        setState({ status: 'anonymous', account: null, error: '' })
        return false
      }
      setState({ status: 'error', account: null, error: error.message || 'N\u00e3o foi poss\u00edvel validar sua sess\u00e3o.' })
      return false
    }
  }, [])

  const clear = useCallback(function clear() {
    requestId.current += 1
    endSession()
    setState({ status: 'anonymous', account: null, error: '' })
  }, [])

  useEffect(() => {
    let mounted = true
    const initialGeneration = getSessionGeneration()
    void Promise.resolve().then(() => {
      if (mounted && initialGeneration === getSessionGeneration()) refresh()
    })
    return () => { mounted = false; requestId.current += 1 }
  }, [refresh])

  return <AuthContext.Provider value={{ ...state, refresh, clear }}>{children}</AuthContext.Provider>
}

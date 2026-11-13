import { parseJson } from './http.js'
import { getAdminAccessToken } from './admin-auth-token.js'
import { refreshAdminAccessToken } from './admin-auth-refresh.js'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function isSafeRetry(options) {
  return SAFE_METHODS.has(String(options.method ?? 'GET').toUpperCase()) && options.retryOnUnauthorized !== false
}

export async function adminFetch(resource, options = {}) {
  const fetchOptions = { ...options }
  delete fetchOptions.retryOnUnauthorized
  const token = getAdminAccessToken()
  const headers = new Headers(fetchOptions.headers ?? {})
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)
  const firstOptions = { ...fetchOptions, credentials: fetchOptions.credentials ?? 'omit', headers }
  const response = await fetch(resource, firstOptions)
  if (response.status !== 401 || !token || !isSafeRetry(options)) return response
  try {
    const nextToken = await refreshAdminAccessToken(token)
    if (!nextToken) return response
    const retryHeaders = new Headers(fetchOptions.headers ?? {})
    retryHeaders.set('Authorization', `Bearer ${nextToken}`)
    return fetch(resource, { ...fetchOptions, credentials: fetchOptions.credentials ?? 'omit', headers: retryHeaders })
  } catch (error) {
    if (error?.status === 401 || error?.status === 403 || error?.code === 'AUTH_CONTRACT_ERROR') return response
    throw error
  }
}

export async function parseAdminResponse(response) { return parseJson(response) }

import { getAccessToken } from './auth-token.js'
import { refreshAccessToken } from './auth-refresh.js'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const AUTHENTICATION_ENDPOINTS = new Set(['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'])
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

export class ApiError extends Error {
  constructor(message, fieldErrors = {}, status) {
    super(message)
    this.name = 'ApiError'
    this.fieldErrors = fieldErrors
    this.status = status
  }
}

function requestPath(resource) {
  const rawUrl = typeof resource === 'string' ? resource : resource.url
  return new URL(rawUrl, 'http://localhost').pathname
}

function prepareRequest(resource, options, token) {
  const headers = new Headers(options.headers ?? {})
  const path = requestPath(resource)
  const hasExplicitAuthorization = headers.has('Authorization')
  const shouldAttachToken = token && !AUTHENTICATION_ENDPOINTS.has(path) && !hasExplicitAuthorization
  if (shouldAttachToken) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return {
    fetchOptions: { ...options, credentials: options.credentials ?? (token ? 'omit' : 'include'), headers },
    usesCurrentToken: Boolean(shouldAttachToken),
  }
}

function isSafeRetry(options) {
  return SAFE_METHODS.has(String(options.method ?? 'GET').toUpperCase())
    && options.retryOnUnauthorized !== false
}

export async function authenticatedFetch(resource, options = {}) {
  const fetchOptions = { ...options }
  delete fetchOptions.retryOnUnauthorized
  const token = getAccessToken()
  const prepared = prepareRequest(resource, fetchOptions, token)
  const response = await fetch(resource, prepared.fetchOptions)
  if (response.status !== 401 || !prepared.usesCurrentToken || !isSafeRetry(options)) return response

  let refreshedToken
  try {
    refreshedToken = refreshAccessToken(token)
    const nextToken = await refreshedToken
    if (!nextToken) return response
    const retryHeaders = new Headers(fetchOptions.headers ?? {})
    retryHeaders.set('Authorization', `Bearer ${nextToken}`)
    return fetch(resource, {
      ...fetchOptions,
      credentials: fetchOptions.credentials ?? 'omit',
      headers: retryHeaders,
    })
  } catch (error) {
    // A definitive refresh failure leaves the original 401 for the API layer;
    // transient failures remain errors and are not mistaken for revocation.
    if (error?.status === 401 || error?.status === 403 || error?.code === 'AUTH_CONTRACT_ERROR') return response
    throw error
  }
}

export function readBearerToken(response, ErrorType = ApiError) {
  const authorization = response.headers.get('authorization')
  const match = /^Bearer\s+(\S+)$/i.exec(authorization ?? '')
  if (!match) {
    const error = new ErrorType('A resposta de autenticação não contém um Bearer válido.', {}, response.status)
    error.code = 'AUTH_CONTRACT_ERROR'
    throw error
  }
  return match[1]
}

export async function parseJson(response) {
  const contentType = response.headers.get('content-type') ?? ''
  return contentType.includes('application/json') ? response.json() : null
}

export async function getCsrfToken(ErrorType, message) {
  const response = await fetch(`${API_BASE_URL}/api/csrf`, { credentials: 'include' })
  const body = await parseJson(response)
  if (!response.ok || !body?.token) throw new ErrorType(message, {}, response.status)
  return body.token
}

export function groupFieldErrors(fieldErrors = []) {
  return fieldErrors.reduce((grouped, { field, message }) => ({
    ...grouped,
    [field]: [...(grouped[field] ?? []), message],
  }), {})
}

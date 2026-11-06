import { getAccessToken } from './auth-token.js'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const AUTHENTICATION_ENDPOINTS = new Set(['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'])

export class ApiError extends Error {
  constructor(message, fieldErrors = {}, status) {
    super(message)
    this.name = 'ApiError'
    this.fieldErrors = fieldErrors
    this.status = status
  }
}

export function authenticatedFetch(resource, options = {}) {
  const headers = new Headers(options.headers ?? {})
  const token = getAccessToken()
  const rawUrl = typeof resource === 'string' ? resource : resource.url
  const path = new URL(rawUrl, 'http://localhost').pathname
  if (token && !AUTHENTICATION_ENDPOINTS.has(path) && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return fetch(resource, { ...options, credentials: options.credentials ?? 'include', headers })
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

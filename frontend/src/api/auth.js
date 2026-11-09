import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson, readBearerToken } from './http.js'
import {
  beginSession,
  clearAccessToken,
  endSession,
  getAccessToken,
  getSessionGeneration,
  setAccessTokenForGeneration,
} from './auth-token.js'

export class AuthApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'AuthApiError'
  }
}

async function authenticatedPost(path, body) {
  try {
  const token = await getCsrfToken(AuthApiError, 'Não foi possível iniciar a solicitação. Tente novamente.')
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST', credentials: 'include',
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  const responseBody = await parseJson(response)
  if (!response.ok) {
    throw new AuthApiError(responseBody?.message ?? 'Não foi possível concluir a solicitação. Tente novamente.', groupFieldErrors(responseBody?.fieldErrors), response.status)
  }
    return response
  } catch (error) {
    if (error instanceof AuthApiError) throw error
    throw new AuthApiError('N\u00e3o foi poss\u00edvel conectar ao servidor. Verifique se a aplica\u00e7\u00e3o est\u00e1 em execu\u00e7\u00e3o.')
  }
}

export async function login(credentials) {
  const expectedGeneration = beginSession()
  try {
    const response = await authenticatedPost('/api/auth/login', credentials)
    const token = readBearerToken(response, AuthApiError)
    if (!setAccessTokenForGeneration(token, expectedGeneration)) throw staleSessionError()
    return token
  } catch (error) {
    if (error instanceof AuthApiError) throw error
    throw new AuthApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

let refreshInFlight = null

function staleSessionError() {
  return new AuthApiError('A sessão de autenticação foi alterada.', {}, 401)
}

function isDefinitiveRefreshFailure(error) {
  return error?.status === 401 || error?.status === 403 || error?.code === 'AUTH_CONTRACT_ERROR'
}

async function refreshAccessTokenOnce(previousToken, expectedGeneration) {
  try {
    const response = await authenticatedPost('/api/auth/refresh')
    const token = readBearerToken(response, AuthApiError)
    if (getSessionGeneration() !== expectedGeneration
      || !setAccessTokenForGeneration(token, expectedGeneration)) throw staleSessionError()
    return token
  } catch (error) {
    if (isDefinitiveRefreshFailure(error)) clearAccessToken(previousToken, expectedGeneration)
    throw error
  }
}

function startRefresh(expectedGeneration, previousToken) {
  if (refreshInFlight?.generation === expectedGeneration) return refreshInFlight.promise

  const promise = refreshAccessTokenOnce(previousToken, expectedGeneration)
  const trackedPromise = promise.finally(() => {
    if (refreshInFlight?.promise === trackedPromise) refreshInFlight = null
  })
  refreshInFlight = { generation: expectedGeneration, promise: trackedPromise }
  return trackedPromise
}

export function refreshAccessToken(failedToken) {
  const currentToken = getAccessToken()
  if (failedToken !== undefined) {
    if (!currentToken) return Promise.reject(staleSessionError())
    if (failedToken !== currentToken) return Promise.resolve(currentToken)
  }
  if (!currentToken) return Promise.reject(staleSessionError())

  return startRefresh(getSessionGeneration(), currentToken)
}

export function restoreSession() {
  return startRefresh(getSessionGeneration(), getAccessToken())
}

export async function logout() {
  endSession()
  try {
    await authenticatedPost('/api/auth/logout')
  } catch (error) {
    if (error instanceof AuthApiError) throw error
    throw new AuthApiError('Não foi possível encerrar a sessão. Tente novamente.')
  }
}

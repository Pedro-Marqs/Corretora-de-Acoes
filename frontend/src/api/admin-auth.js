import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson, readBearerToken } from './http.js'
import {
  beginAdminSession,
  clearAdminAccessToken,
  endAdminSession,
  getAdminAccessToken,
  getAdminSessionGeneration,
  setAdminAccessTokenForGeneration,
} from './admin-auth-token.js'
import { registerAdminRefreshAccessToken } from './admin-auth-refresh.js'

export class AdminAuthApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'AdminAuthApiError'
  }
}

const NETWORK_MESSAGE = 'Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.'
let refreshInFlight = null

async function authenticatedPost(path, body) {
  try {
    const csrfToken = await getCsrfToken(AdminAuthApiError, 'Não foi possível iniciar a autenticação administrativa. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      credentials: 'include',
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), 'X-XSRF-TOKEN': csrfToken },
      ...(body ? { body: JSON.stringify(body) } : {}),
    })
    const responseBody = await parseJson(response)
    if (!response.ok) {
      throw new AdminAuthApiError(responseBody?.message ?? 'Não foi possível concluir a autenticação administrativa.', groupFieldErrors(responseBody?.fieldErrors), response.status)
    }
    return response
  } catch (error) {
    if (error instanceof AdminAuthApiError) throw error
    throw new AdminAuthApiError(NETWORK_MESSAGE)
  }
}

function staleSessionError() { return new AdminAuthApiError('A sessão administrativa foi alterada.', {}, 401) }
function isDefinitiveRefreshFailure(error) { return error?.status === 401 || error?.status === 403 || error?.code === 'AUTH_CONTRACT_ERROR' }

export async function loginAdmin(credentials) {
  const generation = beginAdminSession()
  try {
    const response = await authenticatedPost('/api/admin/auth/login', credentials)
    const token = readBearerToken(response, AdminAuthApiError)
    if (!setAdminAccessTokenForGeneration(token, generation)) throw staleSessionError()
    return token
  } catch (error) {
    if (error instanceof AdminAuthApiError) throw error
    throw new AdminAuthApiError(NETWORK_MESSAGE)
  }
}

async function refreshAdminAccessTokenOnce(previousToken, generation) {
  try {
    const response = await authenticatedPost('/api/admin/auth/refresh')
    const token = readBearerToken(response, AdminAuthApiError)
    if (getAdminSessionGeneration() !== generation || !setAdminAccessTokenForGeneration(token, generation)) throw staleSessionError()
    return token
  } catch (error) {
    if (isDefinitiveRefreshFailure(error)) clearAdminAccessToken(previousToken, generation)
    throw error
  }
}

function startRefresh(generation, previousToken) {
  if (refreshInFlight?.generation === generation) return refreshInFlight.promise
  const promise = refreshAdminAccessTokenOnce(previousToken, generation)
  const trackedPromise = promise.finally(() => {
    if (refreshInFlight?.promise === trackedPromise) refreshInFlight = null
  })
  refreshInFlight = { generation, promise: trackedPromise }
  return trackedPromise
}

export function refreshAdminAccessTokenForCurrentSession(failedToken) {
  const currentToken = getAdminAccessToken()
  if (failedToken !== undefined) {
    if (!currentToken) return Promise.reject(staleSessionError())
    if (failedToken !== currentToken) return Promise.resolve(currentToken)
  }
  return startRefresh(getAdminSessionGeneration(), currentToken)
}

registerAdminRefreshAccessToken(refreshAdminAccessTokenForCurrentSession)

export function restoreAdminSession() {
  return startRefresh(getAdminSessionGeneration(), getAdminAccessToken())
}

export async function logoutAdmin() {
  endAdminSession()
  try {
    await authenticatedPost('/api/admin/auth/logout')
  } catch (error) {
    if (error instanceof AdminAuthApiError) throw error
    throw new AdminAuthApiError('Não foi possível encerrar a sessão administrativa. Tente novamente.')
  }
}

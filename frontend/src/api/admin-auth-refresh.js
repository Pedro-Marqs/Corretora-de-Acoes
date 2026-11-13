import { getAdminAccessToken, getAdminSessionGeneration } from './admin-auth-token.js'

let refreshHandler = null

export function registerAdminRefreshAccessToken(handler) { refreshHandler = handler }

export function refreshAdminAccessToken(failedToken) {
  if (!refreshHandler) return Promise.reject(new Error('O cliente administrativo ainda não foi inicializado.'))
  return refreshHandler(failedToken)
}

export { getAdminAccessToken, getAdminSessionGeneration }

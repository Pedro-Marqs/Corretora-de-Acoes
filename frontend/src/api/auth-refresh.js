let refreshHandler = null

export function registerRefreshAccessToken(handler) {
  refreshHandler = handler
}

export function refreshAccessToken(failedToken) {
  if (!refreshHandler) {
    return Promise.reject(new Error('O cliente de autenticação ainda não foi inicializado.'))
  }
  return refreshHandler(failedToken)
}

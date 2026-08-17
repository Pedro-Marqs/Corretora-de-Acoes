import { API_BASE_URL, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class AuthApiError extends Error {
  constructor(message, fieldErrors = {}, status) {
    super(message)
    this.name = 'AuthApiError'
    this.fieldErrors = fieldErrors
    this.status = status
  }
}

async function authenticatedPost(path, body) {
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
}

export async function login(credentials) {
  try {
    await authenticatedPost('/api/auth/login', credentials)
  } catch (error) {
    if (error instanceof AuthApiError) throw error
    throw new AuthApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function logout() {
  try {
    await authenticatedPost('/api/auth/logout')
  } catch (error) {
    if (error instanceof AuthApiError) throw error
    throw new AuthApiError('Não foi possível encerrar a sessão. Tente novamente.')
  }
}

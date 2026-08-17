import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class AccountApiError extends ApiError {
  constructor(message, fieldErrors = {}) {
    super(message, fieldErrors)
    this.name = 'AccountApiError'
  }
}

export async function getCurrentAccount() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/accounts/me`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) {
      const error = new AccountApiError(body?.message ?? 'Não foi possível consultar a conta.')
      error.status = response.status
      throw error
    }
    if (!body) throw new AccountApiError('A resposta da conta não pôde ser processada.')
    return body
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function createAccount(account) {
  try {
    const csrfToken = await getCsrfToken(AccountApiError, 'Não foi possível iniciar o cadastro. Tente novamente.')

    const response = await fetch(`${API_BASE_URL}/api/accounts`, {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
      body: JSON.stringify(account),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      const fields = groupFieldErrors(body?.fieldErrors)
      throw new AccountApiError(body?.message ?? 'Não foi possível criar a conta. Tente novamente.', fields)
    }
    if (!body) throw new AccountApiError('A resposta do cadastro não pôde ser processada.')
    return body
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

import { API_BASE_URL, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class AccountApiError extends Error {
  constructor(message, fieldErrors = {}) {
    super(message)
    this.name = 'AccountApiError'
    this.fieldErrors = fieldErrors
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

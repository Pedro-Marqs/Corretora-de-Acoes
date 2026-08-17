const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export class AccountApiError extends Error {
  constructor(message, fieldErrors = {}) {
    super(message)
    this.name = 'AccountApiError'
    this.fieldErrors = fieldErrors
  }
}

async function parseJson(response) {
  const contentType = response.headers.get('content-type') ?? ''
  return contentType.includes('application/json') ? response.json() : null
}

export async function createAccount(account) {
  try {
    const csrfResponse = await fetch(`${API_BASE_URL}/api/csrf`, { credentials: 'include' })
    const csrf = await parseJson(csrfResponse)
    if (!csrfResponse.ok || !csrf?.token) throw new AccountApiError('Não foi possível iniciar o cadastro. Tente novamente.')

    const response = await fetch(`${API_BASE_URL}/api/accounts`, {
      method: 'POST', credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf.token },
      body: JSON.stringify(account),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      const fields = (body?.fieldErrors ?? []).reduce((grouped, { field, message }) => ({
        ...grouped,
        [field]: [...(grouped[field] ?? []), message],
      }), {})
      throw new AccountApiError(body?.message ?? 'Não foi possível criar a conta. Tente novamente.', fields)
    }
    if (!body) throw new AccountApiError('A resposta do cadastro não pôde ser processada.')
    return body
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

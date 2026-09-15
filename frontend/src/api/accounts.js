import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class AccountApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
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

async function changeCredential(path, payload) {
  try {
    const token = await getCsrfToken(AccountApiError, 'Não foi possível iniciar a alteração. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}${path}`, { method: 'PATCH', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token }, body: JSON.stringify(payload) })
    const body = await parseJson(response)
    if (!response.ok) throw new AccountApiError(body?.message ?? 'Não foi possível alterar os dados.', groupFieldErrors(body?.fieldErrors), response.status)
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}
export function changeEmail(payload) { return changeCredential('/api/accounts/me/email', payload) }
export function changePassword(payload) { return changeCredential('/api/accounts/me/password', payload) }

export async function deleteAccount(payload) {
  try {
    const token = await getCsrfToken(AccountApiError, 'Não foi possível iniciar a exclusão. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/accounts/me`, {
      method: 'DELETE',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify(payload),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      throw new AccountApiError(
        body?.message ?? 'Não foi possível excluir a conta. Tente novamente.',
        groupFieldErrors(body?.fieldErrors),
        response.status,
      )
    }
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function checkReactivation(cpf) {
  try {
    const token = await getCsrfToken(AccountApiError, 'Não foi possível iniciar a consulta de reativação. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/accounts/reactivation/check`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify({ cpf }),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      throw new AccountApiError(
        body?.message ?? 'Não foi possível consultar a reativação. Tente novamente.',
        groupFieldErrors(body?.fieldErrors),
        response.status,
      )
    }
    if (!body || typeof body !== 'object' || Array.isArray(body) || typeof body.reactivationAvailable !== 'boolean') {
      throw new AccountApiError('A resposta da consulta de reativação não pôde ser processada.')
    }
    return body
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function reactivateAccount(cpf) {
  try {
    const token = await getCsrfToken(AccountApiError, 'Não foi possível iniciar a reativação. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/accounts/reactivation`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify({ cpf }),
    })
    const body = await parseJson(response)
    if (!response.ok) {
      throw new AccountApiError(
        body?.message ?? 'Não foi possível reativar a conta. Tente novamente.',
        groupFieldErrors(body?.fieldErrors),
        response.status,
      )
    }
  } catch (error) {
    if (error instanceof AccountApiError) throw error
    throw new AccountApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

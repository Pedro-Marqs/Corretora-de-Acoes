export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export async function parseJson(response) {
  const contentType = response.headers.get('content-type') ?? ''
  return contentType.includes('application/json') ? response.json() : null
}

export async function getCsrfToken(ErrorType, message) {
  const response = await fetch(`${API_BASE_URL}/api/csrf`, { credentials: 'include' })
  const body = await parseJson(response)
  if (!response.ok || !body?.token) throw new ErrorType(message)
  return body.token
}

export function groupFieldErrors(fieldErrors = []) {
  return fieldErrors.reduce((grouped, { field, message }) => ({
    ...grouped,
    [field]: [...(grouped[field] ?? []), message],
  }), {})
}

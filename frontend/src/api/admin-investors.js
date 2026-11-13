import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors } from './http.js'
import { adminFetch, parseAdminResponse } from './admin-http.js'

export class AdminInvestorApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) {
    super(message, fieldErrors, status)
    this.name = 'AdminInvestorApiError'
  }
}

const STATUSES = ['ACTIVE', 'INACTIVE', 'ALL']
const SORTS = ['createdAt,desc', 'createdAt,asc', 'name,asc', 'name,desc', 'email,asc', 'email,desc', 'status,asc', 'status,desc']

function validInvestor(item) {
  return item && typeof item.id === 'string' && typeof item.name === 'string' && typeof item.emailMasked === 'string'
    && typeof item.cpfMasked === 'string' && ['ACTIVE', 'INACTIVE'].includes(item.status)
}
function validPage(body) {
  return body && Array.isArray(body.content) && body.content.every(validInvestor)
    && Number.isInteger(body.page) && Number.isInteger(body.size) && Number.isInteger(body.totalElements)
    && Number.isInteger(body.totalPages) && body.page >= 0 && body.size > 0 && body.totalElements >= 0 && body.totalPages >= 0
}

function queryError(error) {
  if (error instanceof AdminInvestorApiError) throw error
  throw new AdminInvestorApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
}

export async function listAdminInvestors(filters = {}) {
  const status = STATUSES.includes(filters.status) ? filters.status : 'ALL'
  const sort = SORTS.includes(filters.sort) ? filters.sort : 'createdAt,desc'
  const params = new URLSearchParams({ status, page: String(Math.max(0, Number(filters.page) || 0)), size: String(Math.min(100, Math.max(1, Number(filters.size) || 20))), sort })
  if (filters.search?.trim()) params.set('search', filters.search.trim())
  try {
    const response = await adminFetch(`${API_BASE_URL}/api/admin/investors?${params}`)
    const body = await parseAdminResponse(response)
    if (!response.ok) throw new AdminInvestorApiError(body?.message ?? 'Não foi possível carregar os investidores.', groupFieldErrors(body?.fieldErrors), response.status)
    if (!validPage(body)) throw new AdminInvestorApiError('A resposta da lista de investidores não pôde ser processada.')
    return body
  } catch (error) { return queryError(error) }
}

export async function changeAdminInvestorStatus(accountId, status) {
  try {
    const csrfToken = await getCsrfToken(AdminInvestorApiError, 'Não foi possível iniciar a alteração de status. Tente novamente.')
    const response = await adminFetch(`${API_BASE_URL}/api/admin/investors/${encodeURIComponent(accountId)}/status`, {
      method: 'PATCH',
      retryOnUnauthorized: false,
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
      body: JSON.stringify({ status }),
    })
    const body = await parseAdminResponse(response)
    if (!response.ok) throw new AdminInvestorApiError(body?.message ?? 'Não foi possível alterar o status do investidor.', groupFieldErrors(body?.fieldErrors), response.status)
  } catch (error) { return queryError(error) }
}


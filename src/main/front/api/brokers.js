import { API_BASE_URL, ApiError, getCsrfToken, groupFieldErrors, parseJson } from './http.js'

export class BrokerApiError extends ApiError {
  constructor(message, fieldErrors = {}, status) { super(message, fieldErrors, status); this.name = 'BrokerApiError' }
}

const requiredFields = ['cnpj', 'corporateName', 'tradeName', 'registrationStatus', 'cvmCategory', 'postalCode', 'street', 'district', 'city', 'state']

function requireBroker(body, message, withAssociation = false) {
  const object = body && typeof body === 'object' && !Array.isArray(body)
  const valid = object && requiredFields.every((field) => typeof body[field] === 'string' && body[field].trim().length > 0)
    && /^\d{14}$/.test(body.cnpj)
    && (body.complement === null || typeof body.complement === 'string')
    && (!withAssociation || (typeof body.associationId === 'string' && body.associationId.trim().length > 0))
  if (!valid) throw new BrokerApiError(message)
  return body
}

async function brokerRequest(url, options, fallback, invalid, withAssociation = false) {
  try {
    const response = await fetch(url, options)
    const body = await parseJson(response)
    if (!response.ok) throw new BrokerApiError(body?.message ?? fallback, groupFieldErrors(body?.fieldErrors), response.status)
    return requireBroker(body, invalid, withAssociation)
  } catch (error) {
    if (error instanceof BrokerApiError) throw error
    throw new BrokerApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export function searchBroker(cnpj) {
  return brokerRequest(`${API_BASE_URL}/api/brokers/search?cnpj=${encodeURIComponent(cnpj)}`, { credentials: 'include' },
    'Não foi possível pesquisar a corretora.', 'A resposta da pesquisa não pôde ser processada.')
}

export async function getActiveBrokers() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/brokers`, { credentials: 'include' })
    const body = await parseJson(response)
    if (!response.ok) throw new BrokerApiError(body?.message ?? 'Não foi possível carregar as corretoras.', groupFieldErrors(body?.fieldErrors), response.status)
    if (!Array.isArray(body)) throw new BrokerApiError('A resposta da listagem não pôde ser processada.')
    return body.map((broker) => requireBroker(broker, 'A resposta da listagem não pôde ser processada.', true))
  } catch (error) {
    if (error instanceof BrokerApiError) throw error
    throw new BrokerApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function associateBroker(cnpj) {
  try {
    const token = await getCsrfToken(BrokerApiError, 'Não foi possível iniciar a associação. Tente novamente.')
    return await brokerRequest(`${API_BASE_URL}/api/brokers`, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token }, body: JSON.stringify({ cnpj }) },
      'Não foi possível associar a corretora.', 'A resposta da associação não pôde ser processada.', true)
  } catch (error) {
    if (error instanceof BrokerApiError) throw error
    throw new BrokerApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

export async function removeBroker(associationId) {
  try {
    const token = await getCsrfToken(BrokerApiError, 'Não foi possível iniciar a remoção. Tente novamente.')
    const response = await fetch(`${API_BASE_URL}/api/brokers/${encodeURIComponent(associationId)}`, { method: 'DELETE', credentials: 'include', headers: { 'X-XSRF-TOKEN': token } })
    const body = await parseJson(response)
    if (!response.ok) throw new BrokerApiError(body?.message ?? 'Não foi possível remover a corretora.', groupFieldErrors(body?.fieldErrors), response.status)
  } catch (error) {
    if (error instanceof BrokerApiError) throw error
    throw new BrokerApiError('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.')
  }
}

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { changeAdminInvestorStatus, listAdminInvestors } from './admin-investors.js'
import { beginAdminSession, endAdminSession, setAdminAccessTokenForGeneration } from './admin-auth-token.js'

function response(body, status = 200) { return { ok: status >= 200 && status < 300, status, headers: new Headers(body ? { 'content-type': 'application/json' } : {}), json: vi.fn().mockResolvedValue(body) } }
const page = { content: [{ id: 'id-1', name: 'Ana', emailMasked: 'a***@example.com', cpfMasked: '123.***.***-00', status: 'ACTIVE', createdAt: '2026-01-01T12:00:00Z', inactivatedAt: null }], page: 0, size: 20, totalElements: 1, totalPages: 1 }

describe('admin investors API', () => {
  beforeEach(() => { vi.restoreAllMocks(); vi.stubGlobal('fetch', vi.fn()); endAdminSession(); const generation = beginAdminSession(); setAdminAccessTokenForGeneration('admin', generation) })

  it('envia filtros, ordenação e bearer administrativo', async () => {
    fetch.mockResolvedValue(response(page))
    await expect(listAdminInvestors({ status: 'ACTIVE', search: 'Ana', page: 1, size: 20, sort: 'name,asc' })).resolves.toEqual(page)
    expect(fetch.mock.calls[0][0]).toContain('/api/admin/investors?status=ACTIVE&page=1&size=20&sort=name%2Casc&search=Ana')
    expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer admin')
  })

  it('envia CSRF e retorna conflito de transição', async () => {
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response({ message: 'Conflito' }, 409))
    await expect(changeAdminInvestorStatus('id-1', 'INACTIVE')).rejects.toMatchObject({ status: 409, message: 'Conflito' })
    expect(fetch.mock.calls[1][1].headers.get('X-XSRF-TOKEN')).toBe('csrf')
    expect(fetch.mock.calls[1][1].method).toBe('PATCH')
  })

  it('aplica defaults seguros e aceita lista vazia', async () => {
    const empty = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    fetch.mockResolvedValue(response(empty))
    await expect(listAdminInvestors({ status: 'BAD', sort: 'password,asc', page: -1, size: 1000, search: '  ' })).resolves.toEqual(empty)
    expect(fetch.mock.calls[0][0]).toContain('status=ALL&page=0&size=100&sort=createdAt%2Cdesc')
  })

  it('rejeita resposta inválida e preserva erro de autorização', async () => {
    fetch.mockResolvedValueOnce(response({ content: [{}], page: 0, size: 20, totalElements: 1, totalPages: 1 }))
    await expect(listAdminInvestors()).rejects.toMatchObject({ name: 'AdminInvestorApiError' })
    fetch.mockResolvedValueOnce(response({ message: 'Acesso negado.' }, 403))
    await expect(listAdminInvestors()).rejects.toMatchObject({ status: 403, message: 'Acesso negado.' })
  })

  it('normaliza falha de rede na listagem e no CSRF da mutação', async () => {
    fetch.mockRejectedValueOnce(new TypeError('network details'))
    await expect(listAdminInvestors()).rejects.toMatchObject({ message: expect.stringContaining('Não foi possível conectar') })
    fetch.mockRejectedValueOnce(new TypeError('csrf network'))
    await expect(changeAdminInvestorStatus('id-1', 'ACTIVE')).rejects.toMatchObject({ message: expect.stringContaining('Não foi possível conectar') })
  })

  it('usa mensagem neutra quando o servidor não envia mensagem', async () => {
    fetch.mockResolvedValueOnce(response(null, 500))
    await expect(listAdminInvestors()).rejects.toMatchObject({ status: 500, message: expect.stringContaining('Não foi possível carregar') })
    fetch.mockResolvedValueOnce(response({ token: 'csrf' })).mockResolvedValueOnce(response(null, 401))
    await expect(changeAdminInvestorStatus('id-1', 'ACTIVE')).rejects.toMatchObject({ status: 401, message: expect.stringContaining('Não foi possível alterar') })
  })
})

import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchAttrappe } from '../test/fetchAttrappe.ts'
import { api, ApiFehler } from './client.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('api()', () => {
  it('holt das CSRF-Token, wenn das Cookie fehlt, und schickt es als Header', async () => {
    const attrappe = fetchAttrappe({
      'GET /api/auth/csrf': () => {
        // Das Backend setzt das Cookie; im Test übernimmt die Attrappe das.
        document.cookie = 'XSRF-TOKEN=neu%3D'
        return { status: 200, json: { headerName: 'X-XSRF-TOKEN', token: 'egal' } }
      },
      'POST /api/projects': { status: 201, json: { id: 9 } },
    })

    const antwort = await api<{ id: number }>('/api/projects', { methode: 'POST', daten: { name: 'X' } })

    expect(antwort.id).toBe(9)
    const [, init] = attrappe.mock.calls.find(([pfad]) => pfad === '/api/projects')!
    expect(init?.headers).toMatchObject({ 'X-XSRF-TOKEN': 'neu=', 'Content-Type': 'application/json' })
  })

  it('schickt bei GET kein CSRF-Token', async () => {
    const attrappe = fetchAttrappe({ 'GET /api/projects': { status: 200, json: [] } })
    await api('/api/projects')
    expect(attrappe).toHaveBeenCalledTimes(1)
    expect(attrappe.mock.calls[0][1]?.headers).not.toHaveProperty('X-XSRF-TOKEN')
  })

  it('macht aus einem ProblemDetail einen ApiFehler mit Feldfehlern', async () => {
    document.cookie = 'XSRF-TOKEN=t'
    fetchAttrappe({
      'POST /api/users': {
        status: 400,
        json: { status: 400, detail: 'Eingaben sind ungültig.', fehler: [{ feld: 'passwort', meldung: 'zu kurz' }] },
      },
    })

    const fehler = await api('/api/users', { methode: 'POST', daten: {} }).catch((e: unknown) => e)

    expect(fehler).toBeInstanceOf(ApiFehler)
    expect((fehler as ApiFehler).status).toBe(400)
    expect((fehler as ApiFehler).message).toBe('Eingaben sind ungültig.')
    expect((fehler as ApiFehler).fuerFeld('passwort')).toBe('zu kurz')
  })

  it('liefert bei 204 undefined', async () => {
    document.cookie = 'XSRF-TOKEN=t'
    fetchAttrappe({ 'DELETE /api/tasks/1': { status: 204 } })
    await expect(api('/api/tasks/1', { methode: 'DELETE' })).resolves.toBeUndefined()
  })
})

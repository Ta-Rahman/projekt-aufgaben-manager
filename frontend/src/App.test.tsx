import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.tsx'
import type { AngemeldeterBenutzer } from './api/typen.ts'
import { SitzungProvider } from './auth/Sitzung.tsx'
import { fetchAttrappe } from './test/fetchAttrappe.ts'

const anna: AngemeldeterBenutzer = { id: 1, benutzername: 'admin', anzeigename: 'Anna Admin', rolle: 'ADMIN' }
const mia: AngemeldeterBenutzer = { id: 4, benutzername: 'mitarbeiter', anzeigename: 'Mia Mitarbeiter', rolle: 'MITARBEITER' }

function starteApp(pfad: string) {
  return render(
    <SitzungProvider>
      <MemoryRouter initialEntries={[pfad]}>
        <App />
      </MemoryRouter>
    </SitzungProvider>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('Anmeldung und geschützte Routen', () => {
  it('leitet ohne Sitzung zur Login-Seite um', async () => {
    fetchAttrappe({ 'GET /api/auth/me': { status: 401 } })
    starteApp('/projekte')
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument()
  })

  it('meldet an und führt zur ursprünglich gewünschten Seite', async () => {
    document.cookie = 'XSRF-TOKEN=token-123'
    let angemeldet = false
    const attrappe = fetchAttrappe({
      'GET /api/auth/me': () => (angemeldet ? { status: 200, json: anna } : { status: 401 }),
      'POST /api/auth/login': () => {
        angemeldet = true
        return { status: 204 }
      },
    })
    starteApp('/benutzer')

    await userEvent.type(await screen.findByLabelText(/Benutzername/), 'admin')
    await userEvent.type(screen.getByLabelText(/Passwort/), 'demo')
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }))

    expect(await screen.findByRole('heading', { name: 'Benutzer' })).toBeInTheDocument()
    expect(screen.getByText('Anna Admin (ADMIN)')).toBeInTheDocument()

    // Login als Formular mit CSRF-Header aus dem Cookie (Double-Submit).
    const [, init] = attrappe.mock.calls.find(([pfad]) => pfad === '/api/auth/login')!
    expect(init?.headers).toMatchObject({ 'X-XSRF-TOKEN': 'token-123' })
    expect(String(init?.body)).toBe('username=admin&password=demo')
  })

  it('zeigt bei falschen Zugangsdaten eine unspezifische Meldung', async () => {
    document.cookie = 'XSRF-TOKEN=token-123'
    fetchAttrappe({ 'GET /api/auth/me': { status: 401 }, 'POST /api/auth/login': { status: 401 } })
    starteApp('/login')

    await userEvent.type(await screen.findByLabelText(/Benutzername/), 'admin')
    await userEvent.type(screen.getByLabelText(/Passwort/), 'falsch')
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }))

    expect(await screen.findByText('Benutzername oder Passwort falsch')).toBeInTheDocument()
  })

  it('zeigt den Menüpunkt Benutzer nur für ADMIN und sperrt die Seite für andere', async () => {
    fetchAttrappe({ 'GET /api/auth/me': { status: 200, json: mia } })
    starteApp('/benutzer')

    expect(await screen.findByRole('heading', { name: 'Seite nicht gefunden' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Projekte' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Benutzer' })).not.toBeInTheDocument()
  })

  it('leitet / auf die Projektübersicht um', async () => {
    fetchAttrappe({ 'GET /api/auth/me': { status: 200, json: anna } })
    starteApp('/')
    expect(await screen.findByRole('heading', { name: 'Projekte' })).toBeInTheDocument()
  })

  it('meldet ab und kehrt zur Login-Seite zurück', async () => {
    document.cookie = 'XSRF-TOKEN=token-123'
    fetchAttrappe({ 'GET /api/auth/me': { status: 200, json: mia }, 'POST /api/auth/logout': { status: 204 } })
    starteApp('/projekte')

    await userEvent.click(await screen.findByRole('button', { name: 'Abmelden' }))
    expect(await screen.findByRole('heading', { name: 'Anmelden' })).toBeInTheDocument()
  })

  it('zeigt bei unbekanntem Pfad die 404-Seite', async () => {
    fetchAttrappe({ 'GET /api/auth/me': { status: 200, json: mia } })
    starteApp('/gibt-es-nicht')
    expect(await screen.findByRole('heading', { name: 'Seite nicht gefunden' })).toBeInTheDocument()
  })
})

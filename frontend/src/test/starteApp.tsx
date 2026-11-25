import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import App from '../App.tsx'
import type { AngemeldeterBenutzer } from '../api/typen.ts'
import { SitzungProvider } from '../auth/Sitzung.tsx'
import { fetchAttrappe, type Routen } from './fetchAttrappe.ts'

// Startet die ganze App an einer Adresse, angemeldet als der übergebene Benutzer, mit vorbereiteten API-Antworten.

export const BENUTZER = {
  admin: { id: 1, benutzername: 'admin', anzeigename: 'Anna Admin', rolle: 'ADMIN' },
  leitung: { id: 2, benutzername: 'leitung', anzeigename: 'Lukas Leitung', rolle: 'PROJEKTLEITER' },
  mitarbeiter: { id: 4, benutzername: 'mitarbeiter', anzeigename: 'Mia Mitarbeiter', rolle: 'MITARBEITER' },
} satisfies Record<string, AngemeldeterBenutzer>

export function starteApp(pfad: string, benutzer: AngemeldeterBenutzer | null, routen: Routen = {}) {
  // Für ändernde Anfragen liegt ein CSRF-Cookie bereit (sonst würde der Client es erst holen).
  document.cookie = 'XSRF-TOKEN=test-token'
  const attrappe = fetchAttrappe({
    'GET /api/auth/me': benutzer ? { status: 200, json: benutzer } : { status: 401 },
    ...routen,
  })
  render(
    <SitzungProvider>
      <MemoryRouter initialEntries={[pfad]}>
        <App />
      </MemoryRouter>
    </SitzungProvider>,
  )
  return attrappe
}

/** Die Anfragen, die an einen Pfad gingen, mit Methode und JSON-Inhalt. */
export function anfragenAn(attrappe: ReturnType<typeof fetchAttrappe>, methode: string, pfad: string) {
  return attrappe.mock.calls
    .filter(([p, init]) => String(p) === pfad && (init?.method ?? 'GET') === methode)
    .map(([, init]) => (typeof init?.body === 'string' ? JSON.parse(init.body) : undefined))
}

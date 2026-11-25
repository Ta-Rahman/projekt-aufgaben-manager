import { vi } from 'vitest'

// Ersetzt fetch im Test durch eine Attrappe, die je "METHODE /pfad" eine vorbereitete Antwort liefert.
// So laufen die Komponententests ohne Backend; was die Komponente gesendet hat, steht in attrappe.mock.calls.

type Antwort = { status: number; json?: unknown }
export type Routen = Record<string, Antwort | ((body: unknown) => Antwort)>

export function fetchAttrappe(routen: Routen) {
  const attrappe = vi.fn(async (eingabe: RequestInfo | URL, init?: RequestInit) => {
    const methode = init?.method ?? 'GET'
    const pfad = String(eingabe)
    const eintrag = routen[`${methode} ${pfad}`]
    if (!eintrag) {
      return new Response(null, { status: 404 })
    }
    const body = typeof init?.body === 'string' ? JSON.parse(init.body) : init?.body
    const { status, json } = typeof eintrag === 'function' ? eintrag(body) : eintrag
    return new Response(json === undefined ? null : JSON.stringify(json), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', attrappe)
  return attrappe
}

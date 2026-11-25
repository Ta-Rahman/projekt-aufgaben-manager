// Typen der JSON-Antworten des Backends. Die Feldnamen entsprechen den Java-Records (DTOs).

export type Rolle = 'ADMIN' | 'PROJEKTLEITER' | 'MITARBEITER'

/** GET /api/auth/me */
export type AngemeldeterBenutzer = {
  id: number
  benutzername: string
  anzeigename: string
  rolle: Rolle
}

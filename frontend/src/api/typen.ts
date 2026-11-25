// Typen der JSON-Antworten und -Anfragen des Backends. Die Feldnamen entsprechen den Java-Records (DTOs).
// Datumsfelder kommen als ISO-Text: LocalDate "2026-12-01", Instant "2026-11-25T09:00:00Z".

export type Rolle = 'ADMIN' | 'PROJEKTLEITER' | 'MITARBEITER'

export type AufgabenStatus = 'OFFEN' | 'IN_BEARBEITUNG' | 'ERLEDIGT'

/** GET /api/auth/me */
export type AngemeldeterBenutzer = {
  id: number
  benutzername: string
  anzeigename: string
  rolle: Rolle
}

/** GET /api/users (nur ADMIN) */
export type Benutzer = { id: number; benutzername: string; anzeigename: string; rolle: Rolle; aktiv: boolean }

/** GET /api/users/auswahl */
export type BenutzerAuswahl = { id: number; anzeigename: string; rolle: Rolle }

export type Fortschritt = { erledigt: number; gesamt: number; prozent: number }

/** GET /api/projects */
export type ProjektZeile = {
  id: number
  name: string
  leitung: string
  startDatum: string | null
  endDatum: string | null
  archiviert: boolean
  fortschritt: Fortschritt
}

/** GET /api/projects/{id} */
export type ProjektDetail = {
  id: number
  name: string
  beschreibung: string | null
  startDatum: string | null
  endDatum: string | null
  leitung: { id: number; anzeigename: string }
  archiviertAm: string | null
  version: number
  fortschritt: Fortschritt
  darfVerwalten: boolean
}

/** POST /api/projects, PUT /api/projects/{id} */
export type ProjektDaten = {
  name: string
  beschreibung: string | null
  startDatum: string | null
  endDatum: string | null
  leitungId: number | null
  version?: number
}

export type Mitglied = { id: number; anzeigename: string; benutzername: string; rolle: Rolle; aktiv: boolean }

/** GET /api/projects/{id}/members */
export type Mitglieder = { leitung: Mitglied; mitglieder: Mitglied[] }

export type AufgabePerson = { id: number; anzeigename: string; aktiv: boolean }

/** GET /api/projects/{id}/tasks */
export type Aufgabe = {
  id: number
  titel: string
  beschreibung: string | null
  status: AufgabenStatus
  faelligAm: string | null
  zustaendig: AufgabePerson | null
  erstelltVon: AufgabePerson
  version: number
  darfLoeschen: boolean
}

/** POST /api/projects/{id}/tasks, PUT /api/tasks/{id} */
export type AufgabeDaten = {
  titel: string
  beschreibung: string | null
  zustaendigId: number | null
  faelligAm: string | null
  status: AufgabenStatus | null
  version?: number
}

export const STATUS_TEXT: Record<AufgabenStatus, string> = {
  OFFEN: 'Offen',
  IN_BEARBEITUNG: 'In Bearbeitung',
  ERLEDIGT: 'Erledigt',
}

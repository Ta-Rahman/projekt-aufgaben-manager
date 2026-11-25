// Schmaler Wrapper um fetch für die REST-API des Backends.
//
// - Gleiche Origin: Der Browser schickt das HttpOnly-Sitzungscookie JSESSIONID automatisch mit.
// - CSRF: Bei ändernden Anfragen (POST, PUT, PATCH, DELETE) wird der Wert des Cookies XSRF-TOKEN als Header
//   X-XSRF-TOKEN mitgeschickt (Double-Submit-Cookie, siehe SecurityConfig im Backend). Fehlt das Cookie, etwa
//   direkt nach dem Login (Spring Security tauscht das Token dabei aus), wird es vorher über
//   GET /api/auth/csrf geholt.
// - Fehler kommen vom Backend als RFC-9457-ProblemDetail und werden zu einem ApiFehler.

export type Feldfehler = { feld: string; meldung: string }

/** Fehlerantwort des Backends, z. B. 400 mit Feldfehlern, 403, 404 oder 409. */
export class ApiFehler extends Error {
  readonly status: number
  readonly feldfehler: Feldfehler[]

  constructor(status: number, meldung: string, feldfehler: Feldfehler[] = []) {
    super(meldung)
    this.name = 'ApiFehler'
    this.status = status
    this.feldfehler = feldfehler
  }

  /** Meldung zu einem Eingabefeld, falls das Backend eine geliefert hat. */
  fuerFeld(feld: string): string | undefined {
    return this.feldfehler.find((f) => f.feld === feld)?.meldung
  }
}

const AENDERNDE_METHODEN = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function leseCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((eintrag) => eintrag.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

async function csrfToken(): Promise<string> {
  let token = leseCookie('XSRF-TOKEN')
  if (!token) {
    // Setzt das Cookie; danach steht es in document.cookie.
    await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    token = leseCookie('XSRF-TOKEN')
  }
  return token ? decodeURIComponent(token) : ''
}

async function fehlerAus(antwort: Response): Promise<ApiFehler> {
  const standardMeldung = `Anfrage fehlgeschlagen (${antwort.status})`
  try {
    const problem = (await antwort.json()) as { detail?: string; fehler?: Feldfehler[] }
    return new ApiFehler(antwort.status, problem.detail ?? standardMeldung, problem.fehler ?? [])
  } catch {
    // z. B. 401/403 von Spring Security ohne JSON-Inhalt
    return new ApiFehler(antwort.status, standardMeldung)
  }
}

type Optionen = {
  methode?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  /** Wird als JSON gesendet. */
  daten?: unknown
  /** Wird als application/x-www-form-urlencoded gesendet (nur Login). */
  formular?: Record<string, string>
}

/** Führt eine API-Anfrage aus und liefert den JSON-Inhalt der Antwort (bei 204 undefined). */
export async function api<T>(pfad: string, { methode = 'GET', daten, formular }: Optionen = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  let body: BodyInit | undefined
  if (daten !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(daten)
  } else if (formular) {
    body = new URLSearchParams(formular)
  }
  if (AENDERNDE_METHODEN.has(methode)) {
    headers['X-XSRF-TOKEN'] = await csrfToken()
  }

  const antwort = await fetch(pfad, { method: methode, headers, body, credentials: 'same-origin' })
  if (!antwort.ok) {
    throw await fehlerAus(antwort)
  }
  if (antwort.status === 204) {
    return undefined as T
  }
  return (await antwort.json()) as T
}

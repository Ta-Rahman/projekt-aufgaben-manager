import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import type { AngemeldeterBenutzer } from '../api/typen.ts'

// Sitzungszustand für die ganze App (React Context). Die eigentliche Sitzung hält der Server
// (HttpOnly-Cookie); das Frontend fragt beim Start nur GET /api/auth/me, wer angemeldet ist.

type SitzungWert = {
  /** undefined = wird noch geladen, null = nicht angemeldet */
  benutzer: AngemeldeterBenutzer | null | undefined
  /** true nach ausdrücklichem Abmelden: Die nächste Anmeldung startet dann auf der Projektübersicht. */
  abgemeldet: boolean
  anmelden: (benutzername: string, passwort: string) => Promise<void>
  abmelden: () => Promise<void>
}

const SitzungKontext = createContext<SitzungWert | null>(null)

async function ladeBenutzer(): Promise<AngemeldeterBenutzer | null> {
  try {
    return await api<AngemeldeterBenutzer>('/api/auth/me')
  } catch (fehler) {
    if (fehler instanceof ApiFehler && fehler.status === 401) {
      return null
    }
    throw fehler
  }
}

export function SitzungProvider({ children }: { children: ReactNode }) {
  const [benutzer, setBenutzer] = useState<AngemeldeterBenutzer | null | undefined>(undefined)
  const [abgemeldet, setAbgemeldet] = useState(false)

  useEffect(() => {
    let aktiv = true
    ladeBenutzer()
      .then((b) => aktiv && setBenutzer(b))
      .catch(() => aktiv && setBenutzer(null))
    // Aufräumen, falls die Komponente vor der Antwort verschwindet (z. B. StrictMode im Dev-Modus).
    return () => {
      aktiv = false
    }
  }, [])

  const anmelden = useCallback(async (benutzername: string, passwort: string) => {
    // Formular-Login von Spring Security: form-urlencoded, Antwort 204 oder 401.
    await api<void>('/api/auth/login', { methode: 'POST', formular: { username: benutzername, password: passwort } })
    setBenutzer(await ladeBenutzer())
    setAbgemeldet(false)
  }, [])

  const abmelden = useCallback(async () => {
    await api<void>('/api/auth/logout', { methode: 'POST' })
    setAbgemeldet(true)
    setBenutzer(null)
  }, [])

  const wert = useMemo(
    () => ({ benutzer, abgemeldet, anmelden, abmelden }),
    [benutzer, abgemeldet, anmelden, abmelden],
  )
  return <SitzungKontext.Provider value={wert}>{children}</SitzungKontext.Provider>
}

// Hook und Provider gehören zusammen, daher in einer Datei (die Lint-Regel will sonst nur Komponenten je Datei).
// oxlint-disable-next-line react/only-export-components
export function useSitzung(): SitzungWert {
  const wert = useContext(SitzungKontext)
  if (!wert) {
    throw new Error('useSitzung nur innerhalb von <SitzungProvider> verwenden')
  }
  return wert
}

import { useCallback, useEffect, useState } from 'react'
import { api, ApiFehler } from './client.ts'

// Lädt Daten per GET und hält Ergebnis und Fehler fest. neuLaden() holt die Daten nach einer Änderung erneut
// (z. B. Fortschritt nach einem Statuswechsel). Bewusst einfach: kein Cache, keine Wiederholung.
//
// „laedt“ wird nicht als eigener Zustand gesetzt, sondern abgeleitet: Es lädt, solange das letzte Ergebnis
// zu einem anderen Pfad gehört (z. B. direkt nach dem Umschalten des Archivfilters).

export type Abfrage<T> = {
  daten: T | undefined
  fehler: ApiFehler | undefined
  laedt: boolean
  neuLaden: () => Promise<void>
}

type Ergebnis<T> = { pfad: string; daten?: T; fehler?: ApiFehler }

async function laden<T>(pfad: string): Promise<Ergebnis<T>> {
  try {
    return { pfad, daten: await api<T>(pfad) }
  } catch (e) {
    return { pfad, fehler: e instanceof ApiFehler ? e : new ApiFehler(0, 'Server nicht erreichbar') }
  }
}

export function useAbfrage<T>(pfad: string): Abfrage<T> {
  const [ergebnis, setErgebnis] = useState<Ergebnis<T>>()

  const neuLaden = useCallback(async () => setErgebnis(await laden<T>(pfad)), [pfad])

  // Erstes Laden und Laden nach Pfadwechsel. „aktiv“ verhindert, dass eine verspätete Antwort für einen
  // alten Pfad (schnelles Umschalten des Filters) das neuere Ergebnis überschreibt.
  useEffect(() => {
    let aktiv = true
    laden<T>(pfad).then((e) => aktiv && setErgebnis(e))
    return () => {
      aktiv = false
    }
  }, [pfad])

  const aktuell = ergebnis?.pfad === pfad
  return {
    daten: aktuell ? ergebnis.daten : undefined,
    fehler: aktuell ? ergebnis.fehler : undefined,
    laedt: !aktuell,
    neuLaden,
  }
}

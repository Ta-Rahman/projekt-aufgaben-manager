// Datumsanzeige im deutschen Format. LocalDate kommt als "2026-12-01"; es wird bewusst ohne new Date(...)
// zerlegt, weil new Date("2026-12-01") als UTC-Mitternacht gilt und je nach Zeitzone auf den Vortag rutschen kann.

/** "2026-12-01" → "01.12.2026" (lang) bzw. "01.12.26" (kurz) bzw. "01.12." (ohneJahr) */
export function datum(iso: string | null, art: 'lang' | 'kurz' | 'ohneJahr' = 'lang'): string {
  if (!iso) {
    return ''
  }
  const [jahr, monat, tag] = iso.slice(0, 10).split('-')
  if (art === 'ohneJahr') {
    return `${tag}.${monat}.`
  }
  return `${tag}.${monat}.${art === 'kurz' ? jahr.slice(2) : jahr}`
}

/** Zeitraum eines Projekts, "–" wenn weder Beginn noch Ende gesetzt sind. */
export function zeitraum(start: string | null, ende: string | null, art: 'lang' | 'kurz' = 'lang'): string {
  if (!start && !ende) {
    return '–'
  }
  return `${datum(start, art) || '…'} – ${datum(ende, art) || '…'}`
}

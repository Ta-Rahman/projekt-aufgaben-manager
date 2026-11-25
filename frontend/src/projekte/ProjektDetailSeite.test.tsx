import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Aufgabe, Mitglieder, ProjektDetail } from '../api/typen.ts'
import type { Routen } from '../test/fetchAttrappe.ts'
import { anfragenAn, BENUTZER, starteApp } from '../test/starteApp.tsx'

function projekt(aenderung: Partial<ProjektDetail> = {}): ProjektDetail {
  return {
    id: 1,
    name: 'Relaunch Kundenportal',
    beschreibung: null,
    startDatum: '2026-11-02',
    endDatum: '2027-01-29',
    leitung: { id: 2, anzeigename: 'Lukas Leitung' },
    archiviertAm: null,
    version: 0,
    fortschritt: { erledigt: 1, gesamt: 3, prozent: 33 },
    darfVerwalten: false,
    ...aenderung,
  }
}

const AUFGABEN: Aufgabe[] = [
  {
    id: 11,
    titel: 'Login an Kunden-SSO anbinden',
    beschreibung: null,
    status: 'OFFEN',
    faelligAm: '2026-12-04',
    zustaendig: { id: 4, anzeigename: 'Mia Mitarbeiter', aktiv: true },
    erstelltVon: { id: 2, anzeigename: 'Lukas Leitung', aktiv: true },
    version: 3,
    darfLoeschen: false,
  },
  {
    id: 12,
    titel: 'Altdaten prüfen',
    beschreibung: null,
    status: 'IN_BEARBEITUNG',
    faelligAm: null,
    zustaendig: { id: 6, anzeigename: 'Erik Ehemalig', aktiv: false },
    erstelltVon: { id: 4, anzeigename: 'Mia Mitarbeiter', aktiv: true },
    version: 0,
    darfLoeschen: true,
  },
  {
    id: 13,
    titel: 'Wireframes',
    beschreibung: null,
    status: 'ERLEDIGT',
    faelligAm: null,
    zustaendig: null,
    erstelltVon: { id: 2, anzeigename: 'Lukas Leitung', aktiv: true },
    version: 1,
    darfLoeschen: false,
  },
]

const MITGLIEDER: Mitglieder = {
  leitung: { id: 2, anzeigename: 'Lukas Leitung', benutzername: 'leitung', rolle: 'PROJEKTLEITER', aktiv: true },
  mitglieder: [
    { id: 6, anzeigename: 'Erik Ehemalig', benutzername: 'ehemalig', rolle: 'MITARBEITER', aktiv: false },
    { id: 4, anzeigename: 'Mia Mitarbeiter', benutzername: 'mitarbeiter', rolle: 'MITARBEITER', aktiv: true },
  ],
}

function routen(p: ProjektDetail | (() => ProjektDetail), weitere: Routen = {}): Routen {
  return {
    'GET /api/projects/1': () => ({ status: 200, json: typeof p === 'function' ? p() : p }),
    'GET /api/projects/1/tasks': { status: 200, json: AUFGABEN },
    'GET /api/projects/1/members': { status: 200, json: MITGLIEDER },
    ...weitere,
  }
}

afterEach(() => vi.unstubAllGlobals())

describe('Projektdetail (wf_03, wf_04)', () => {
  it('zeigt Kopf, Fortschritt und Board mit drei Spalten', async () => {
    starteApp('/projekte/1', BENUTZER.mitarbeiter, routen(projekt()))

    expect(await screen.findByRole('heading', { name: 'Relaunch Kundenportal' })).toBeInTheDocument()
    expect(screen.getByText('Leitung: Lukas Leitung · 02.11.2026 – 29.01.2027')).toBeInTheDocument()
    expect(screen.getByText('1 von 3 Aufgaben erledigt (33 %)')).toBeInTheDocument()

    const offen = screen.getByRole('region', { name: 'Offen' })
    expect(within(offen).getByText('Offen (1)')).toBeInTheDocument()
    expect(within(offen).getByText('Mia Mitarbeiter · fällig 04.12.')).toBeInTheDocument()
    const inArbeit = screen.getByRole('region', { name: 'In Bearbeitung' })
    expect(within(inArbeit).getByText('Erik Ehemalig (deaktiviert)')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Erledigt' })).getByText('nicht zugewiesen')).toBeInTheDocument()

    // Mitglied: keine Verwaltungsaktionen, aber neue Aufgaben (Rechtematrix).
    expect(screen.queryByRole('button', { name: 'Bearbeiten' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Archivieren' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Neue Aufgabe/ })).toBeInTheDocument()
  })

  it('ändert den Status per Auswahl mit Version und lädt den Fortschritt neu', async () => {
    let erledigt = 1
    const attrappe = starteApp(
      '/projekte/1',
      BENUTZER.mitarbeiter,
      routen(() => projekt({ fortschritt: { erledigt, gesamt: 3, prozent: Math.round((100 * erledigt) / 3) } }), {
        'PATCH /api/tasks/11/status': () => {
          erledigt = 2
          return { status: 200, json: { ...AUFGABEN[0], status: 'ERLEDIGT', version: 4 } }
        },
      }),
    )

    await userEvent.click(await screen.findByRole('combobox', { name: 'Status von Login an Kunden-SSO anbinden' }))
    await userEvent.click(screen.getByRole('option', { name: 'Erledigt' }))

    expect(await screen.findByText('2 von 3 Aufgaben erledigt (67 %)')).toBeInTheDocument()
    expect(anfragenAn(attrappe, 'PATCH', '/api/tasks/11/status')).toEqual([{ status: 'ERLEDIGT', version: 3 }])
  })

  it('zeigt einen Konflikt (409) verständlich an', async () => {
    starteApp(
      '/projekte/1',
      BENUTZER.mitarbeiter,
      routen(projekt(), {
        'PATCH /api/tasks/11/status': {
          status: 409,
          json: { status: 409, detail: 'Der Datensatz wurde inzwischen von jemand anderem geändert. Bitte neu laden.' },
        },
      }),
    )

    await userEvent.click(await screen.findByRole('combobox', { name: 'Status von Login an Kunden-SSO anbinden' }))
    await userEvent.click(screen.getByRole('option', { name: 'In Bearbeitung' }))

    expect(await screen.findByText(/inzwischen von jemand anderem geändert/)).toBeInTheDocument()
  })

  it('archiviertes Projekt ist schreibgeschützt, Leitung kann wiederherstellen', async () => {
    let archiviertAm: string | null = '2026-01-15T09:00:00Z'
    const attrappe = starteApp(
      '/projekte/1',
      BENUTZER.leitung,
      routen(() => projekt({ darfVerwalten: true, archiviertAm }), {
        'POST /api/projects/1/restore': () => {
          archiviertAm = null
          return { status: 200, json: projekt({ darfVerwalten: true }) }
        },
      }),
    )

    expect(await screen.findByText(/Archiviert am 15.01.2026/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Neue Aufgabe/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Bearbeiten' })).not.toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Status von Wireframes' })).toHaveAttribute('aria-disabled', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Wiederherstellen' }))

    expect(await screen.findByRole('button', { name: 'Archivieren' })).toBeInTheDocument()
    expect(anfragenAn(attrappe, 'POST', '/api/projects/1/restore')).toHaveLength(1)
  })

  it('Reiter Mitglieder: Leitung oben, Entfernen für die Leitung', async () => {
    const attrappe = starteApp(
      '/projekte/1',
      BENUTZER.leitung,
      routen(projekt({ darfVerwalten: true }), { 'DELETE /api/projects/1/members/6': { status: 200, json: MITGLIEDER } }),
    )

    await userEvent.click(await screen.findByRole('tab', { name: 'Mitglieder (2)' }))

    const tabelle = screen.getByRole('table', { name: 'Mitglieder' })
    const zeilen = within(tabelle).getAllByRole('row')
    expect(within(zeilen[1]).getByText('Lukas Leitung')).toBeInTheDocument()
    expect(within(zeilen[1]).getByText('Leitung')).toBeInTheDocument()
    expect(within(zeilen[2]).getByText('Erik Ehemalig (deaktiviert)')).toBeInTheDocument()

    await userEvent.click(within(zeilen[2]).getByRole('button', { name: 'Entfernen' }))
    expect(anfragenAn(attrappe, 'DELETE', '/api/projects/1/members/6')).toHaveLength(1)
  })

  it('unsichtbares Projekt zeigt „Seite nicht gefunden“', async () => {
    starteApp('/projekte/1', BENUTZER.mitarbeiter, {
      'GET /api/projects/1': { status: 404, json: { status: 404, detail: 'Projekt nicht gefunden' } },
      'GET /api/projects/1/tasks': { status: 404 },
      'GET /api/projects/1/members': { status: 404 },
    })

    expect(await screen.findByRole('heading', { name: 'Seite nicht gefunden' })).toBeInTheDocument()
  })
})

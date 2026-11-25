import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { ProjektDetail, ProjektZeile } from '../api/typen.ts'
import { anfragenAn, BENUTZER, starteApp } from '../test/starteApp.tsx'

const KUNDENPORTAL: ProjektZeile = {
  id: 1,
  name: 'Relaunch Kundenportal',
  leitung: 'Lukas Leitung',
  startDatum: '2026-11-02',
  endDatum: '2027-01-29',
  archiviert: false,
  fortschritt: { erledigt: 2, gesamt: 5, prozent: 40 },
}

const INVENTUR: ProjektZeile = {
  id: 3,
  name: 'Inventur Hardware 2025',
  leitung: 'Lukas Leitung',
  startDatum: null,
  endDatum: null,
  archiviert: true,
  fortschritt: { erledigt: 2, gesamt: 2, prozent: 100 },
}

afterEach(() => vi.unstubAllGlobals())

describe('Projektübersicht (wf_02)', () => {
  it('zeigt Projekte mit Leitung, Zeitraum und Fortschritt', async () => {
    starteApp('/projekte', BENUTZER.mitarbeiter, { 'GET /api/projects?archiviert=false': { status: 200, json: [KUNDENPORTAL] } })

    const zeile = (await screen.findByRole('link', { name: 'Relaunch Kundenportal' })).closest('tr')!
    expect(within(zeile).getByText('Lukas Leitung')).toBeInTheDocument()
    expect(within(zeile).getByText('02.11.26 – 29.01.27')).toBeInTheDocument()
    expect(within(zeile).getByText('40 %')).toBeInTheDocument()
    expect(within(zeile).getByText('2 / 5')).toBeInTheDocument()
    // Mitarbeitende legen keine Projekte an.
    expect(screen.queryByRole('button', { name: /Neues Projekt/ })).not.toBeInTheDocument()
  })

  it('wechselt zum Filter Archiviert', async () => {
    starteApp('/projekte', BENUTZER.leitung, {
      'GET /api/projects?archiviert=false': { status: 200, json: [KUNDENPORTAL] },
      'GET /api/projects?archiviert=true': { status: 200, json: [INVENTUR] },
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Archiviert' }))

    expect(await screen.findByRole('link', { name: 'Inventur Hardware 2025' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Relaunch Kundenportal' })).not.toBeInTheDocument()
    expect(screen.getByText('–')).toBeInTheDocument()
  })

  it('Projektleitung legt ein Projekt an und landet im Projektdetail', async () => {
    const neu: ProjektDetail = {
      id: 9,
      name: 'Schulung Datenschutz',
      beschreibung: null,
      startDatum: null,
      endDatum: null,
      leitung: { id: 2, anzeigename: 'Lukas Leitung' },
      archiviertAm: null,
      version: 0,
      fortschritt: { erledigt: 0, gesamt: 0, prozent: 0 },
      darfVerwalten: true,
    }
    const attrappe = starteApp('/projekte', BENUTZER.leitung, {
      'GET /api/projects?archiviert=false': { status: 200, json: [] },
      'POST /api/projects': { status: 201, json: neu },
      'GET /api/projects/9': { status: 200, json: neu },
      'GET /api/projects/9/tasks': { status: 200, json: [] },
      'GET /api/projects/9/members': {
        status: 200,
        json: { leitung: { id: 2, anzeigename: 'Lukas Leitung', benutzername: 'leitung', rolle: 'PROJEKTLEITER', aktiv: true }, mitglieder: [] },
      },
    })

    await userEvent.click(await screen.findByRole('button', { name: /Neues Projekt/ }))
    await userEvent.type(screen.getByLabelText(/Name/), 'Schulung Datenschutz')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(await screen.findByRole('heading', { name: 'Schulung Datenschutz' })).toBeInTheDocument()
    // Projektleitung: kein Auswahlfeld Leitung, das Backend setzt sie selbst ein.
    expect(anfragenAn(attrappe, 'POST', '/api/projects')[0]).toMatchObject({ name: 'Schulung Datenschutz', leitungId: 2 })
  })
})

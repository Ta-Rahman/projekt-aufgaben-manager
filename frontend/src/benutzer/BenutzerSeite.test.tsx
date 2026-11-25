import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Benutzer } from '../api/typen.ts'
import { anfragenAn, BENUTZER, starteApp } from '../test/starteApp.tsx'

const LISTE: Benutzer[] = [
  { id: 1, benutzername: 'admin', anzeigename: 'Anna Admin', rolle: 'ADMIN', aktiv: true },
  { id: 6, benutzername: 'ehemalig', anzeigename: 'Erik Ehemalig', rolle: 'MITARBEITER', aktiv: false },
  { id: 4, benutzername: 'mitarbeiter', anzeigename: 'Mia Mitarbeiter', rolle: 'MITARBEITER', aktiv: true },
]

function zeile(name: string) {
  return screen.getByRole('cell', { name }).closest('tr')!
}

afterEach(() => vi.unstubAllGlobals())

describe('Benutzerverwaltung (wf_05)', () => {
  it('listet Benutzer; das eigene Konto kann nicht deaktiviert werden', async () => {
    starteApp('/benutzer', BENUTZER.admin, { 'GET /api/users': { status: 200, json: LISTE } })

    expect(await screen.findByRole('cell', { name: 'Mia Mitarbeiter' })).toBeInTheDocument()
    expect(within(zeile('Erik Ehemalig')).getByText('deaktiviert')).toBeInTheDocument()
    expect(within(zeile('Erik Ehemalig')).getByRole('button', { name: 'Aktivieren' })).toBeInTheDocument()
    expect(within(zeile('Mia Mitarbeiter')).getByRole('button', { name: 'Deaktivieren' })).toBeInTheDocument()
    expect(within(zeile('Anna Admin')).queryByRole('button', { name: 'Deaktivieren' })).not.toBeInTheDocument()
  })

  it('deaktiviert einen Benutzer per PUT und lädt neu', async () => {
    const attrappe = starteApp('/benutzer', BENUTZER.admin, {
      'GET /api/users': { status: 200, json: LISTE },
      'PUT /api/users/4': { status: 200, json: { ...LISTE[2], aktiv: false } },
    })

    await screen.findByRole('cell', { name: 'Mia Mitarbeiter' })
    await userEvent.click(within(zeile('Mia Mitarbeiter')).getByRole('button', { name: 'Deaktivieren' }))

    expect(anfragenAn(attrappe, 'PUT', '/api/users/4')).toEqual([
      { anzeigename: 'Mia Mitarbeiter', rolle: 'MITARBEITER', aktiv: false, passwort: null },
    ])
    expect(anfragenAn(attrappe, 'GET', '/api/users').length).toBe(2)
  })

  it('legt einen Benutzer an und zeigt Feldfehler des Backends', async () => {
    const attrappe = starteApp('/benutzer', BENUTZER.admin, {
      'GET /api/users': { status: 200, json: LISTE },
      'POST /api/users': {
        status: 400,
        json: { status: 400, detail: 'Eingaben sind ungültig.', fehler: [{ feld: 'passwort', meldung: 'Passwort muss 8 bis 72 Zeichen lang sein' }] },
      },
    })

    await userEvent.click(await screen.findByRole('button', { name: /Neuer Benutzer/ }))
    await userEvent.type(screen.getByLabelText(/Benutzername/), 'neu.person')
    await userEvent.type(screen.getByLabelText(/^Name/), 'Neue Person')
    await userEvent.type(screen.getByLabelText(/^Passwort/), 'kurz')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(await screen.findByText('Passwort muss 8 bis 72 Zeichen lang sein')).toBeInTheDocument()
    expect(anfragenAn(attrappe, 'POST', '/api/users')[0]).toEqual({
      benutzername: 'neu.person',
      anzeigename: 'Neue Person',
      rolle: 'MITARBEITER',
      passwort: 'kurz',
    })
  })
})

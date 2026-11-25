import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField } from '@mui/material'
import { useState, type FormEvent } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import type { Benutzer, Rolle } from '../api/typen.ts'

type Props = {
  offen: boolean
  /** Gesetzt = Bearbeiten, sonst Anlegen. */
  benutzer?: Benutzer
  /** Eigenes Konto: Rolle ist nicht änderbar (Schutz vor Selbstaussperrung, Backend antwortet sonst 409). */
  istSelbst: boolean
  beiSchliessen: () => void
  beiGespeichert: () => void
}

const ROLLEN: Rolle[] = ['ADMIN', 'PROJEKTLEITER', 'MITARBEITER']

// Benutzer anlegen und bearbeiten (US1). Benutzername ist nach dem Anlegen fest; ein leeres Passwortfeld
// beim Bearbeiten lässt das Passwort unverändert.
export default function BenutzerDialog(props: Props) {
  return (
    <Dialog open={props.offen} onClose={props.beiSchliessen} fullWidth maxWidth="sm">
      <BenutzerFormular {...props} />
    </Dialog>
  )
}

function BenutzerFormular({ benutzer, istSelbst, beiSchliessen, beiGespeichert }: Props) {
  const [benutzername, setBenutzername] = useState(benutzer?.benutzername ?? '')
  const [anzeigename, setAnzeigename] = useState(benutzer?.anzeigename ?? '')
  const [rolle, setRolle] = useState<Rolle>(benutzer?.rolle ?? 'MITARBEITER')
  const [passwort, setPasswort] = useState('')
  const [fehler, setFehler] = useState<ApiFehler>()
  const [sendet, setSendet] = useState(false)

  async function speichern(ereignis: FormEvent) {
    ereignis.preventDefault()
    setSendet(true)
    try {
      if (benutzer) {
        await api(`/api/users/${benutzer.id}`, {
          methode: 'PUT',
          daten: { anzeigename, rolle, aktiv: benutzer.aktiv, passwort: passwort || null },
        })
      } else {
        await api('/api/users', { methode: 'POST', daten: { benutzername, anzeigename, rolle, passwort } })
      }
      beiGespeichert()
    } catch (e) {
      setFehler(e instanceof ApiFehler ? e : new ApiFehler(0, 'Speichern fehlgeschlagen'))
    } finally {
      setSendet(false)
    }
  }

  const feld = (name: string) => ({ error: !!fehler?.fuerFeld(name), helperText: fehler?.fuerFeld(name) })

  return (
    <form onSubmit={speichern} noValidate>
      <DialogTitle>{benutzer ? 'Benutzer bearbeiten' : 'Neuer Benutzer'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Benutzername"
            value={benutzername}
            onChange={(e) => setBenutzername(e.target.value)}
            disabled={!!benutzer}
            required
            autoFocus={!benutzer}
            {...feld('benutzername')}
          />
          <TextField
            label="Name"
            value={anzeigename}
            onChange={(e) => setAnzeigename(e.target.value)}
            required
            {...feld('anzeigename')}
          />
          <TextField
            select
            label="Rolle"
            value={rolle}
            onChange={(e) => setRolle(e.target.value as Rolle)}
            disabled={istSelbst}
            helperText={istSelbst ? 'Die eigene Rolle kann nicht geändert werden.' : undefined}
          >
            {ROLLEN.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label={benutzer ? 'Neues Passwort (leer lassen = unverändert)' : 'Passwort'}
            type="password"
            value={passwort}
            onChange={(e) => setPasswort(e.target.value)}
            autoComplete="new-password"
            required={!benutzer}
            {...feld('passwort')}
          />
          {fehler && fehler.feldfehler.length === 0 && <Alert severity="error">{fehler.message}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={beiSchliessen}>Abbrechen</Button>
        <Button
          type="submit"
          variant="contained"
          disabled={sendet || !anzeigename.trim() || (!benutzer && (!benutzername || !passwort))}
        >
          Speichern
        </Button>
      </DialogActions>
    </form>
  )
}

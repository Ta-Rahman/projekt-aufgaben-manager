import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
} from '@mui/material'
import { useState, type ReactNode } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import type { BenutzerAuswahl, Mitglied, Mitglieder } from '../api/typen.ts'

type Props = {
  projektId: number
  mitglieder: Mitglieder
  darfVerwalten: boolean
  beiGeaendert: () => void
}

// Reiter Mitglieder (US3, Wireframe wf_04). Die Leitung steht oben mit Kennzeichen „Leitung“, ist aber kein
// Mitglied und nicht entfernbar (D2). Deaktivierte erscheinen grau (D6).
export default function MitgliederReiter({ projektId, mitglieder, darfVerwalten, beiGeaendert }: Props) {
  const [fehler, setFehler] = useState<string>()
  const [dialogOffen, setDialogOffen] = useState(false)

  async function entfernen(m: Mitglied) {
    setFehler(undefined)
    try {
      await api(`/api/projects/${projektId}/members/${m.id}`, { methode: 'DELETE' })
      beiGeaendert()
    } catch (e) {
      setFehler(e instanceof ApiFehler ? e.message : 'Entfernen fehlgeschlagen')
    }
  }

  return (
    <>
      {darfVerwalten && (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
          <Button variant="contained" onClick={() => setDialogOffen(true)}>
            + Mitglied hinzufügen
          </Button>
        </Box>
      )}
      {fehler && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {fehler}
        </Alert>
      )}
      <TableContainer component={Paper} variant="outlined">
        <Table aria-label="Mitglieder">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Benutzername</TableCell>
              <TableCell>Rolle</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            <PersonZeile person={mitglieder.leitung}>
              <Chip label="Leitung" color="warning" size="small" />
            </PersonZeile>
            {mitglieder.mitglieder.map((m) => (
              <PersonZeile key={m.id} person={m}>
                {darfVerwalten && (
                  <Button size="small" variant="outlined" onClick={() => entfernen(m)}>
                    Entfernen
                  </Button>
                )}
              </PersonZeile>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <MitgliedHinzufuegenDialog
        offen={dialogOffen}
        projektId={projektId}
        vorhanden={[mitglieder.leitung.id, ...mitglieder.mitglieder.map((m) => m.id)]}
        beiSchliessen={() => setDialogOffen(false)}
        beiHinzugefuegt={() => {
          setDialogOffen(false)
          beiGeaendert()
        }}
      />
    </>
  )
}

function PersonZeile({ person, children }: { person: Mitglied; children?: ReactNode }) {
  const farbe = person.aktiv ? undefined : 'text.disabled'
  return (
    <TableRow>
      <TableCell sx={{ color: farbe }}>
        {person.anzeigename}
        {!person.aktiv && ' (deaktiviert)'}
      </TableCell>
      <TableCell sx={{ color: farbe }}>{person.benutzername}</TableCell>
      <TableCell sx={{ color: farbe }}>{person.rolle}</TableCell>
      <TableCell align="right">{children}</TableCell>
    </TableRow>
  )
}

function MitgliedHinzufuegenDialog({
  offen,
  projektId,
  vorhanden,
  beiSchliessen,
  beiHinzugefuegt,
}: {
  offen: boolean
  projektId: number
  vorhanden: number[]
  beiSchliessen: () => void
  beiHinzugefuegt: () => void
}) {
  const [auswahl, setAuswahl] = useState<BenutzerAuswahl[]>([])
  const [benutzerId, setBenutzerId] = useState<number | ''>('')
  const [fehler, setFehler] = useState<string>()

  async function oeffnen() {
    setBenutzerId('')
    setFehler(undefined)
    // Nur aktive Benutzer (D6), ohne Leitung und vorhandene Mitglieder.
    const alle = await api<BenutzerAuswahl[]>('/api/users/auswahl')
    setAuswahl(alle.filter((b) => !vorhanden.includes(b.id)))
  }

  async function hinzufuegen() {
    try {
      await api(`/api/projects/${projektId}/members`, { methode: 'POST', daten: { benutzerId } })
      beiHinzugefuegt()
    } catch (e) {
      setFehler(e instanceof ApiFehler ? e.message : 'Hinzufügen fehlgeschlagen')
    }
  }

  return (
    <Dialog open={offen} onClose={beiSchliessen} fullWidth maxWidth="xs" slotProps={{ transition: { onEnter: oeffnen } }}>
      <DialogTitle>Mitglied hinzufügen</DialogTitle>
      <DialogContent>
        <TextField
          select
          label="Benutzer"
          value={benutzerId}
          onChange={(e) => setBenutzerId(Number(e.target.value))}
          fullWidth
          sx={{ mt: 1 }}
        >
          {auswahl.map((b) => (
            <MenuItem key={b.id} value={b.id}>
              {b.anzeigename} ({b.rolle})
            </MenuItem>
          ))}
        </TextField>
        {auswahl.length === 0 && (
          <Alert severity="info" sx={{ mt: 2 }}>
            Alle aktiven Benutzer sind bereits im Projekt.
          </Alert>
        )}
        {fehler && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {fehler}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={beiSchliessen}>Abbrechen</Button>
        <Button variant="contained" onClick={hinzufuegen} disabled={benutzerId === ''}>
          Hinzufügen
        </Button>
      </DialogActions>
    </Dialog>
  )
}

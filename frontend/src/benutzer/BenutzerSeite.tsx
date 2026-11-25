import {
  Alert,
  Box,
  Button,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import type { Benutzer } from '../api/typen.ts'
import { useAbfrage } from '../api/useAbfrage.ts'
import { useSitzung } from '../auth/Sitzung.tsx'
import BenutzerDialog from './BenutzerDialog.tsx'

// Benutzerverwaltung (US1, Wireframe wf_05), nur ADMIN. Kein Löschen: Deaktivierte können sich nicht anmelden
// und sind nicht mehr auswählbar, behalten aber ihre Zuordnungen (D5, D6).
export default function BenutzerSeite() {
  const { benutzer: angemeldet } = useSitzung()
  const liste = useAbfrage<Benutzer[]>('/api/users')
  const [dialog, setDialog] = useState<{ offen: boolean; benutzer?: Benutzer }>({ offen: false })
  const [meldung, setMeldung] = useState<string>()

  async function aktivUmschalten(b: Benutzer) {
    setMeldung(undefined)
    try {
      await api(`/api/users/${b.id}`, {
        methode: 'PUT',
        daten: { anzeigename: b.anzeigename, rolle: b.rolle, aktiv: !b.aktiv, passwort: null },
      })
    } catch (e) {
      setMeldung(e instanceof ApiFehler ? e.message : 'Ändern fehlgeschlagen')
    }
    await liste.neuLaden()
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Benutzer
        </Typography>
        <Button variant="contained" onClick={() => setDialog({ offen: true })}>
          + Neuer Benutzer
        </Button>
      </Box>
      {liste.fehler && <Alert severity="error">{liste.fehler.message}</Alert>}
      {meldung && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setMeldung(undefined)}>
          {meldung}
        </Alert>
      )}
      {liste.daten && (
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Benutzer">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Benutzername</TableCell>
                <TableCell>Rolle</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {liste.daten.map((b) => {
                const farbe = b.aktiv ? undefined : 'text.disabled'
                const istSelbst = b.id === angemeldet?.id
                return (
                  <TableRow key={b.id}>
                    <TableCell sx={{ color: farbe }}>{b.anzeigename}</TableCell>
                    <TableCell sx={{ color: farbe }}>{b.benutzername}</TableCell>
                    <TableCell sx={{ color: farbe }}>{b.rolle}</TableCell>
                    <TableCell sx={{ color: farbe }}>{b.aktiv ? 'aktiv' : 'deaktiviert'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                        <Button size="small" variant="outlined" onClick={() => setDialog({ offen: true, benutzer: b })}>
                          Bearbeiten
                        </Button>
                        {/* Das eigene Konto lässt sich nicht deaktivieren (Backend: 409). */}
                        {!istSelbst && (
                          <Button size="small" variant="outlined" onClick={() => aktivUmschalten(b)}>
                            {b.aktiv ? 'Deaktivieren' : 'Aktivieren'}
                          </Button>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 2 }}>
        Kein Löschen: Deaktivierte können sich nicht anmelden und sind nicht mehr auswählbar.
      </Typography>
      <BenutzerDialog
        offen={dialog.offen}
        benutzer={dialog.benutzer}
        istSelbst={dialog.benutzer?.id === angemeldet?.id}
        beiSchliessen={() => setDialog({ offen: false })}
        beiGespeichert={() => {
          setDialog({ offen: false })
          void liste.neuLaden()
        }}
      />
    </>
  )
}

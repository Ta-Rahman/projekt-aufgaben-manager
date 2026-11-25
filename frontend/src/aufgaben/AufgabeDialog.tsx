import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useState, type FormEvent } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import { STATUS_TEXT, type Aufgabe, type AufgabeDaten, type AufgabenStatus, type Mitglied } from '../api/typen.ts'
import Bestaetigung from '../komponenten/Bestaetigung.tsx'

type Props = {
  offen: boolean
  projektId: number
  /** Gesetzt = Bearbeiten, sonst Anlegen. */
  aufgabe?: Aufgabe
  /** Leitung und Mitglieder des Projekts; zuständig werden können nur die aktiven (Regel aus den Wireframes). */
  personen: Mitglied[]
  beiSchliessen: () => void
  beiGeaendert: () => void
}

// Aufgabe anlegen, bearbeiten, löschen (US4, Wireframe wf_06). Formular als eigene Komponente im Dialog,
// damit jedes Öffnen mit frischem Zustand aus den Props beginnt (siehe ProjektDialog).
export default function AufgabeDialog(props: Props) {
  return (
    <Dialog open={props.offen} onClose={props.beiSchliessen} fullWidth maxWidth="sm">
      <AufgabeFormular {...props} />
    </Dialog>
  )
}

function AufgabeFormular({ projektId, aufgabe, personen, beiSchliessen, beiGeaendert }: Props) {
  const [titel, setTitel] = useState(aufgabe?.titel ?? '')
  const [beschreibung, setBeschreibung] = useState(aufgabe?.beschreibung ?? '')
  const [zustaendigId, setZustaendigId] = useState<number | ''>(aufgabe?.zustaendig?.id ?? '')
  const [faellig, setFaellig] = useState(aufgabe?.faelligAm ?? '')
  const [status, setStatus] = useState<AufgabenStatus>(aufgabe?.status ?? 'OFFEN')
  const [fehler, setFehler] = useState<ApiFehler>()
  const [sendet, setSendet] = useState(false)
  const [loeschenFragen, setLoeschenFragen] = useState(false)

  // Auswahl: aktive Personen; eine bisher zuständige, inzwischen deaktivierte Person bleibt sichtbar (D6).
  const optionen = personen.filter((p) => p.aktiv || p.id === aufgabe?.zustaendig?.id)

  async function ausfuehren(aktion: () => Promise<unknown>) {
    setSendet(true)
    try {
      await aktion()
      beiGeaendert()
    } catch (e) {
      setFehler(e instanceof ApiFehler ? e : new ApiFehler(0, 'Speichern fehlgeschlagen'))
    } finally {
      setSendet(false)
    }
  }

  function speichern(ereignis: FormEvent) {
    ereignis.preventDefault()
    const daten: AufgabeDaten = {
      titel,
      beschreibung: beschreibung || null,
      zustaendigId: zustaendigId === '' ? null : zustaendigId,
      faelligAm: faellig || null,
      status,
      version: aufgabe?.version,
    }
    void ausfuehren(() =>
      aufgabe
        ? api(`/api/tasks/${aufgabe.id}`, { methode: 'PUT', daten })
        : api(`/api/projects/${projektId}/tasks`, { methode: 'POST', daten }),
    )
  }

  return (
    <>
      <form onSubmit={speichern} noValidate>
        <DialogTitle>{aufgabe ? 'Aufgabe bearbeiten' : 'Neue Aufgabe'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Titel"
              value={titel}
              onChange={(e) => setTitel(e.target.value)}
              required
              autoFocus
              error={!!fehler?.fuerFeld('titel')}
              helperText={fehler?.fuerFeld('titel')}
            />
            <TextField
              label="Beschreibung"
              value={beschreibung}
              onChange={(e) => setBeschreibung(e.target.value)}
              multiline
              minRows={3}
            />
            <TextField
              select
              label="Zuständig"
              value={zustaendigId}
              onChange={(e) => setZustaendigId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">
                <em>nicht zugewiesen</em>
              </MenuItem>
              {optionen.map((p) => (
                <MenuItem key={p.id} value={p.id} disabled={!p.aktiv}>
                  {p.anzeigename}
                  {!p.aktiv && ' (deaktiviert)'}
                </MenuItem>
              ))}
            </TextField>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Fällig am"
                type="date"
                value={faellig}
                onChange={(e) => setFaellig(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                select
                label="Status"
                value={status}
                onChange={(e) => setStatus(e.target.value as AufgabenStatus)}
                fullWidth
              >
                {Object.entries(STATUS_TEXT).map(([wert, text]) => (
                  <MenuItem key={wert} value={wert}>
                    {text}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
            <Typography variant="caption" color="text.secondary">
              Zuständig: nur aktive Mitglieder und Leitung wählbar
            </Typography>
            {fehler && fehler.feldfehler.length === 0 && <Alert severity="error">{fehler.message}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          {aufgabe?.darfLoeschen && (
            <Button color="error" variant="outlined" onClick={() => setLoeschenFragen(true)} disabled={sendet}>
              Löschen
            </Button>
          )}
          <Box sx={{ flexGrow: 1 }} />
          <Button onClick={beiSchliessen}>Abbrechen</Button>
          <Button type="submit" variant="contained" disabled={sendet || !titel.trim()}>
            Speichern
          </Button>
        </DialogActions>
      </form>
      <Bestaetigung
        offen={loeschenFragen}
        titel="Aufgabe löschen?"
        text={`„${aufgabe?.titel}“ wird endgültig gelöscht und zählt nicht mehr zum Fortschritt.`}
        bestaetigen="Löschen"
        gefaehrlich
        beiAbbruch={() => setLoeschenFragen(false)}
        beiBestaetigung={() => {
          setLoeschenFragen(false)
          void ausfuehren(() => api(`/api/tasks/${aufgabe!.id}`, { methode: 'DELETE' }))
        }}
      />
    </>
  )
}

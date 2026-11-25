import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiFehler } from '../api/client.ts'
import type { BenutzerAuswahl, ProjektDaten, ProjektDetail } from '../api/typen.ts'
import { useSitzung } from '../auth/Sitzung.tsx'

type Props = {
  offen: boolean
  /** Gesetzt = Bearbeiten, sonst Anlegen. */
  projekt?: ProjektDetail
  beiSchliessen: () => void
  beiGespeichert: (projekt: ProjektDetail) => void
}

// Projekt anlegen und bearbeiten (US2) als Dialog (P3). Die Leitung kann ADMIN immer wählen, die Leitung selbst
// beim Bearbeiten (Übergabe). Eine Projektleitung legt Projekte nur für sich selbst an; das Feld fehlt dann.
//
// Das Formular ist eine eigene Komponente im Dialog: MUI hängt den Dialoginhalt erst beim Öffnen ein. Jeder
// Öffnen-Vorgang beginnt so mit frischem Zustand aus den Props, ohne Effekt, der Felder zurücksetzt.
export default function ProjektDialog(props: Props) {
  return (
    <Dialog open={props.offen} onClose={props.beiSchliessen} fullWidth maxWidth="sm">
      <ProjektFormular {...props} />
    </Dialog>
  )
}

function ProjektFormular({ projekt, beiSchliessen, beiGespeichert }: Props) {
  const { benutzer } = useSitzung()
  const [name, setName] = useState(projekt?.name ?? '')
  const [beschreibung, setBeschreibung] = useState(projekt?.beschreibung ?? '')
  const [start, setStart] = useState(projekt?.startDatum ?? '')
  const [ende, setEnde] = useState(projekt?.endDatum ?? '')
  const [leitungId, setLeitungId] = useState<number | ''>(projekt?.leitung.id ?? benutzer?.id ?? '')
  const [auswahl, setAuswahl] = useState<BenutzerAuswahl[]>([])
  const [fehler, setFehler] = useState<ApiFehler>()
  const [sendet, setSendet] = useState(false)

  const leitungWaehlbar = benutzer?.rolle === 'ADMIN' || projekt !== undefined

  // Mögliche Leitungen laden (aktive Benutzer mit Rolle ADMIN oder PROJEKTLEITER).
  useEffect(() => {
    if (leitungWaehlbar) {
      api<BenutzerAuswahl[]>('/api/users/auswahl')
        .then((liste) => setAuswahl(liste.filter((b) => b.rolle !== 'MITARBEITER')))
        .catch(() => setAuswahl([]))
    }
  }, [leitungWaehlbar])

  async function speichern(ereignis: FormEvent) {
    ereignis.preventDefault()
    setSendet(true)
    const daten: ProjektDaten = {
      name,
      beschreibung: beschreibung || null,
      startDatum: start || null,
      endDatum: ende || null,
      leitungId: leitungId === '' ? null : leitungId,
      version: projekt?.version,
    }
    try {
      const gespeichert = projekt
        ? await api<ProjektDetail>(`/api/projects/${projekt.id}`, { methode: 'PUT', daten })
        : await api<ProjektDetail>('/api/projects', { methode: 'POST', daten })
      beiGespeichert(gespeichert)
    } catch (e) {
      setFehler(e instanceof ApiFehler ? e : new ApiFehler(0, 'Speichern fehlgeschlagen'))
    } finally {
      setSendet(false)
    }
  }

  return (
    <form onSubmit={speichern} noValidate>
        <DialogTitle>{projekt ? 'Projekt bearbeiten' : 'Neues Projekt'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
              error={!!fehler?.fuerFeld('name')}
              helperText={fehler?.fuerFeld('name')}
            />
            <TextField
              label="Beschreibung"
              value={beschreibung}
              onChange={(e) => setBeschreibung(e.target.value)}
              multiline
              minRows={3}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="Beginn"
                type="date"
                value={start}
                onChange={(e) => setStart(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Ende"
                type="date"
                value={ende}
                onChange={(e) => setEnde(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
            </Stack>
            {leitungWaehlbar && (
              <TextField
                select
                label="Leitung"
                value={auswahl.length ? leitungId : ''}
                onChange={(e) => setLeitungId(Number(e.target.value))}
              >
                {auswahl.map((b) => (
                  <MenuItem key={b.id} value={b.id}>
                    {b.anzeigename} ({b.rolle})
                  </MenuItem>
                ))}
              </TextField>
            )}
            {fehler && fehler.feldfehler.length === 0 && <Alert severity="error">{fehler.message}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={beiSchliessen}>Abbrechen</Button>
          <Button type="submit" variant="contained" disabled={sendet || !name.trim()}>
            Speichern
          </Button>
        </DialogActions>
    </form>
  )
}

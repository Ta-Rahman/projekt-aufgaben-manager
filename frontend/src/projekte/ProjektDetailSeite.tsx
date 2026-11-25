import { Alert, Box, Button, CircularProgress, Link, Stack, Tab, Tabs, Typography } from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router'
import { api, ApiFehler } from '../api/client.ts'
import type { Aufgabe, AufgabenStatus, Mitglieder, ProjektDetail } from '../api/typen.ts'
import { useAbfrage } from '../api/useAbfrage.ts'
import AufgabeDialog from '../aufgaben/AufgabeDialog.tsx'
import AufgabenBoard from '../aufgaben/AufgabenBoard.tsx'
import Bestaetigung from '../komponenten/Bestaetigung.tsx'
import FortschrittBalken from '../komponenten/FortschrittBalken.tsx'
import { datum, zeitraum } from '../format.ts'
import NichtGefunden from '../pages/NichtGefunden.tsx'
import MitgliederReiter from './MitgliederReiter.tsx'
import ProjektDialog from './ProjektDialog.tsx'

// Projektdetail (Wireframes wf_03, wf_04): Kopf mit Fortschritt und Aktionen, Reiter Aufgaben und Mitglieder (P2).
// Archivierte Projekte sind schreibgeschützt: Hinweis, keine Bearbeiten-Aktionen, nur „Wiederherstellen“ (P6).
export default function ProjektDetailSeite() {
  const { id } = useParams()
  const projekt = useAbfrage<ProjektDetail>(`/api/projects/${id}`)
  const aufgaben = useAbfrage<Aufgabe[]>(`/api/projects/${id}/tasks`)
  const mitglieder = useAbfrage<Mitglieder>(`/api/projects/${id}/members`)

  const [reiter, setReiter] = useState<'aufgaben' | 'mitglieder'>('aufgaben')
  const [projektDialog, setProjektDialog] = useState(false)
  const [archivFrage, setArchivFrage] = useState(false)
  const [aufgabeDialog, setAufgabeDialog] = useState<{ offen: boolean; aufgabe?: Aufgabe }>({ offen: false })
  const [meldung, setMeldung] = useState<string>()

  if (projekt.fehler?.status === 404) {
    return <NichtGefunden />
  }
  if (!projekt.daten || !aufgaben.daten || !mitglieder.daten) {
    return projekt.fehler ? (
      <Alert severity="error">{projekt.fehler.message}</Alert>
    ) : (
      <CircularProgress aria-label="Lädt" />
    )
  }

  const p = projekt.daten
  const archiviert = p.archiviertAm !== null
  const personen = [mitglieder.daten.leitung, ...mitglieder.daten.mitglieder]

  /** Nach jeder Änderung alles neu laden: Fortschritt, Versionen und Listen bleiben so konsistent. */
  async function allesNeuLaden() {
    await Promise.all([projekt.neuLaden(), aufgaben.neuLaden(), mitglieder.neuLaden()])
  }

  /** Führt eine Aktion aus; Fehler (z. B. 409 bei veralteter Version) werden oben angezeigt. */
  async function aktion(ausfuehren: () => Promise<unknown>) {
    setMeldung(undefined)
    try {
      await ausfuehren()
    } catch (e) {
      setMeldung(e instanceof ApiFehler ? e.message : 'Aktion fehlgeschlagen')
    }
    await allesNeuLaden()
  }

  function statusAendern(aufgabe: Aufgabe, status: AufgabenStatus) {
    void aktion(() =>
      api(`/api/tasks/${aufgabe.id}/status`, { methode: 'PATCH', daten: { status, version: aufgabe.version } }),
    )
  }

  return (
    <>
      <Link component={RouterLink} to={archiviert ? '/projekte?archiviert=true' : '/projekte'} underline="hover">
        ← Projekte
      </Link>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mt: 1, gap: 2 }}>
        <Box>
          <Typography variant="h4" component="h1">
            {p.name}
          </Typography>
          <Typography color="text.secondary">
            Leitung: {p.leitung.anzeigename} · {zeitraum(p.startDatum, p.endDatum)}
          </Typography>
        </Box>
        {p.darfVerwalten && (
          <Stack direction="row" spacing={1}>
            {archiviert ? (
              <Button
                variant="outlined"
                onClick={() => aktion(() => api(`/api/projects/${p.id}/restore`, { methode: 'POST' }))}
              >
                Wiederherstellen
              </Button>
            ) : (
              <>
                <Button variant="outlined" onClick={() => setProjektDialog(true)}>
                  Bearbeiten
                </Button>
                <Button variant="outlined" onClick={() => setArchivFrage(true)}>
                  Archivieren
                </Button>
              </>
            )}
          </Stack>
        )}
      </Box>

      {p.beschreibung && <Typography sx={{ mt: 1 }}>{p.beschreibung}</Typography>}
      <Box sx={{ maxWidth: 560, mt: 2 }}>
        <FortschrittBalken
          fortschritt={p.fortschritt}
          text={`${p.fortschritt.erledigt} von ${p.fortschritt.gesamt} Aufgaben erledigt (${p.fortschritt.prozent} %)`}
        />
      </Box>

      {archiviert && (
        <Alert severity="info" sx={{ mt: 2 }}>
          Archiviert am {datum(p.archiviertAm)}. Das Projekt ist schreibgeschützt.
        </Alert>
      )}
      {meldung && (
        <Alert severity="warning" sx={{ mt: 2 }} onClose={() => setMeldung(undefined)}>
          {meldung}
        </Alert>
      )}

      <Tabs value={reiter} onChange={(_, wert) => setReiter(wert)} sx={{ mt: 3, mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab value="aufgaben" label="Aufgaben" />
        <Tab value="mitglieder" label={`Mitglieder (${mitglieder.daten.mitglieder.length})`} />
      </Tabs>

      {reiter === 'aufgaben' ? (
        <>
          {!archiviert && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
              <Button variant="contained" onClick={() => setAufgabeDialog({ offen: true })}>
                + Neue Aufgabe
              </Button>
            </Box>
          )}
          <AufgabenBoard
            aufgaben={aufgaben.daten}
            schreibgeschuetzt={archiviert}
            beiStatuswechsel={statusAendern}
            beiOeffnen={(a) => setAufgabeDialog({ offen: true, aufgabe: a })}
          />
        </>
      ) : (
        <MitgliederReiter
          projektId={p.id}
          mitglieder={mitglieder.daten}
          darfVerwalten={p.darfVerwalten && !archiviert}
          beiGeaendert={allesNeuLaden}
        />
      )}

      <ProjektDialog
        offen={projektDialog}
        projekt={p}
        beiSchliessen={() => setProjektDialog(false)}
        beiGespeichert={() => {
          setProjektDialog(false)
          void allesNeuLaden()
        }}
      />
      <AufgabeDialog
        offen={aufgabeDialog.offen}
        projektId={p.id}
        aufgabe={aufgabeDialog.aufgabe}
        personen={personen}
        beiSchliessen={() => setAufgabeDialog({ offen: false })}
        beiGeaendert={() => {
          setAufgabeDialog({ offen: false })
          void allesNeuLaden()
        }}
      />
      <Bestaetigung
        offen={archivFrage}
        titel="Projekt archivieren?"
        text="Archivierte Projekte sind schreibgeschützt und erscheinen nur noch unter „Archiviert“. Wiederherstellen ist jederzeit möglich."
        bestaetigen="Archivieren"
        beiAbbruch={() => setArchivFrage(false)}
        beiBestaetigung={() => {
          setArchivFrage(false)
          void aktion(() => api(`/api/projects/${p.id}/archive`, { methode: 'POST' }))
        }}
      />
    </>
  )
}

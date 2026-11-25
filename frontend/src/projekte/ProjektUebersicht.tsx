import {
  Alert,
  Box,
  Button,
  Link,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router'
import type { ProjektZeile } from '../api/typen.ts'
import { useAbfrage } from '../api/useAbfrage.ts'
import { useSitzung } from '../auth/Sitzung.tsx'
import FortschrittBalken from '../komponenten/FortschrittBalken.tsx'
import { zeitraum } from '../format.ts'
import ProjektDialog from './ProjektDialog.tsx'

// Projektübersicht (Wireframe wf_02, P4): Tabelle der sichtbaren Projekte (US8) mit Fortschritt (US6),
// Filter aktiv/archiviert (P6). Der Filter steht in der Adresse (?archiviert=true), damit Zurück funktioniert.
export default function ProjektUebersicht() {
  const { benutzer } = useSitzung()
  const navigate = useNavigate()
  const [parameter, setParameter] = useSearchParams()
  const archiviert = parameter.get('archiviert') === 'true'
  const { daten: projekte, fehler, laedt } = useAbfrage<ProjektZeile[]>(`/api/projects?archiviert=${archiviert}`)
  const [dialogOffen, setDialogOffen] = useState(false)

  const darfAnlegen = benutzer?.rolle === 'ADMIN' || benutzer?.rolle === 'PROJEKTLEITER'

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 3 }}>
        Projekte
      </Typography>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <ToggleButtonGroup
          value={archiviert ? 'archiviert' : 'aktiv'}
          exclusive
          size="small"
          onChange={(_, wert) => wert && setParameter(wert === 'archiviert' ? { archiviert: 'true' } : {})}
          aria-label="Filter"
        >
          <ToggleButton value="aktiv">Aktiv</ToggleButton>
          <ToggleButton value="archiviert">Archiviert</ToggleButton>
        </ToggleButtonGroup>
        {darfAnlegen && (
          <Button variant="contained" onClick={() => setDialogOffen(true)}>
            + Neues Projekt
          </Button>
        )}
      </Box>

      {fehler && <Alert severity="error">{fehler.message}</Alert>}
      {!laedt && projekte && (
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Projekte">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Leitung</TableCell>
                <TableCell>Zeitraum</TableCell>
                <TableCell sx={{ width: '28%' }}>Fortschritt</TableCell>
                <TableCell align="right">Aufgaben</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {projekte.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    {archiviert ? 'Keine archivierten Projekte.' : 'Keine Projekte. Neue Projekte legt eine Projektleitung an.'}
                  </TableCell>
                </TableRow>
              )}
              {projekte.map((p) => (
                <TableRow key={p.id} hover onClick={() => navigate(`/projekte/${p.id}`)} sx={{ cursor: 'pointer' }}>
                  <TableCell>
                    <Link component={RouterLink} to={`/projekte/${p.id}`} underline="hover">
                      {p.name}
                    </Link>
                  </TableCell>
                  <TableCell>{p.leitung}</TableCell>
                  <TableCell>{zeitraum(p.startDatum, p.endDatum, 'kurz')}</TableCell>
                  <TableCell>
                    <FortschrittBalken fortschritt={p.fortschritt} text={`${p.fortschritt.prozent} %`} />
                  </TableCell>
                  <TableCell align="right">
                    {p.fortschritt.erledigt} / {p.fortschritt.gesamt}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <ProjektDialog
        offen={dialogOffen}
        beiSchliessen={() => setDialogOffen(false)}
        beiGespeichert={(neu) => navigate(`/projekte/${neu.id}`)}
      />
    </>
  )
}

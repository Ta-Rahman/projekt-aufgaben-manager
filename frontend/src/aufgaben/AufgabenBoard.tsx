import { Box, Card, CardActionArea, CardContent, MenuItem, Paper, TextField, Typography } from '@mui/material'
import { STATUS_TEXT, type Aufgabe, type AufgabenStatus } from '../api/typen.ts'
import { datum } from '../format.ts'

type Props = {
  aufgaben: Aufgabe[]
  schreibgeschuetzt: boolean
  beiStatuswechsel: (aufgabe: Aufgabe, status: AufgabenStatus) => void
  beiOeffnen: (aufgabe: Aufgabe) => void
}

const SPALTEN: AufgabenStatus[] = ['OFFEN', 'IN_BEARBEITUNG', 'ERLEDIGT']

// Aufgaben-Board mit drei Statusspalten (P1, Wireframe wf_03). Statuswechsel per Auswahlmenü auf der Karte,
// kein Drag & Drop (E5). Klick auf die Karte öffnet den Aufgabendialog.
export default function AufgabenBoard({ aufgaben, schreibgeschuetzt, beiStatuswechsel, beiOeffnen }: Props) {
  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
      {SPALTEN.map((spalte) => {
        const karten = aufgaben.filter((a) => a.status === spalte)
        return (
          <Paper
            key={spalte}
            variant="outlined"
            component="section"
            aria-label={STATUS_TEXT[spalte]}
            sx={{ p: 1.5, bgcolor: 'grey.50', minHeight: 120 }}
          >
            <Typography variant="subtitle1" component="h2" sx={{ mb: 1, px: 0.5 }}>
              {STATUS_TEXT[spalte]} ({karten.length})
            </Typography>
            <Box sx={{ display: 'grid', gap: 1.5 }}>
              {karten.map((a) => (
                <AufgabeKarte
                  key={a.id}
                  aufgabe={a}
                  schreibgeschuetzt={schreibgeschuetzt}
                  beiStatuswechsel={beiStatuswechsel}
                  beiOeffnen={beiOeffnen}
                />
              ))}
            </Box>
          </Paper>
        )
      })}
    </Box>
  )
}

function AufgabeKarte({
  aufgabe,
  schreibgeschuetzt,
  beiStatuswechsel,
  beiOeffnen,
}: { aufgabe: Aufgabe } & Omit<Props, 'aufgaben'>) {
  const zustaendig = aufgabe.zustaendig
  const zeile = [
    zustaendig ? `${zustaendig.anzeigename}${zustaendig.aktiv ? '' : ' (deaktiviert)'}` : 'nicht zugewiesen',
    aufgabe.faelligAm ? `fällig ${datum(aufgabe.faelligAm, 'ohneJahr')}` : null,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <Card variant="outlined" data-testid={`aufgabe-${aufgabe.id}`}>
      <CardActionArea onClick={() => beiOeffnen(aufgabe)} disabled={schreibgeschuetzt}>
        <CardContent sx={{ pb: 1 }}>
          <Typography variant="body1">{aufgabe.titel}</Typography>
          <Typography variant="body2" color={zustaendig && !zustaendig.aktiv ? 'text.disabled' : 'text.secondary'}>
            {zeile}
          </Typography>
        </CardContent>
      </CardActionArea>
      <Box sx={{ px: 2, pb: 1.5 }}>
        <TextField
          select
          size="small"
          value={aufgabe.status}
          disabled={schreibgeschuetzt}
          onChange={(e) => beiStatuswechsel(aufgabe, e.target.value as AufgabenStatus)}
          slotProps={{ htmlInput: { 'aria-label': `Status von ${aufgabe.titel}` } }}
          sx={{ minWidth: 170 }}
        >
          {SPALTEN.map((s) => (
            <MenuItem key={s} value={s}>
              {STATUS_TEXT[s]}
            </MenuItem>
          ))}
        </TextField>
      </Box>
    </Card>
  )
}

import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material'

type Props = {
  offen: boolean
  titel: string
  text: string
  bestaetigen: string
  gefaehrlich?: boolean
  beiBestaetigung: () => void
  beiAbbruch: () => void
}

// Rückfrage vor Archivieren und Löschen (entwurf/Seiten.md: „Bestätigungen“).
export default function Bestaetigung({ offen, titel, text, bestaetigen, gefaehrlich, beiBestaetigung, beiAbbruch }: Props) {
  return (
    <Dialog open={offen} onClose={beiAbbruch}>
      <DialogTitle>{titel}</DialogTitle>
      <DialogContent>
        <DialogContentText>{text}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={beiAbbruch}>Abbrechen</Button>
        <Button onClick={beiBestaetigung} variant="contained" color={gefaehrlich ? 'error' : 'primary'}>
          {bestaetigen}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

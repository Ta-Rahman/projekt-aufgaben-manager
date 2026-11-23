import { Container, Typography } from '@mui/material'

// Platzhalter-Startseite des Grundgerüsts.
export default function StartSeite() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1">
        Aufgaben-Manager
      </Typography>
      <Typography sx={{ mt: 2 }}>Grundgerüst läuft.</Typography>
    </Container>
  )
}

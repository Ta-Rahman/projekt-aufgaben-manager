import { Button, Container, Typography } from '@mui/material'
import { Link } from 'react-router'

export default function NichtGefunden() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1">
        Seite nicht gefunden
      </Typography>
      <Button component={Link} to="/" sx={{ mt: 2 }}>
        Zur Startseite
      </Button>
    </Container>
  )
}

import { Alert, Box, Button, Card, CardContent, TextField, Typography } from '@mui/material'
import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiFehler } from '../api/client.ts'
import { useSitzung } from '../auth/Sitzung.tsx'

// Login (US7, Wireframe wf_01). Die Fehlermeldung ist bewusst unspezifisch: Sie verrät nicht, ob es den
// Benutzernamen gibt oder ob das Konto deaktiviert ist.

export default function LoginSeite() {
  const { benutzer, anmelden } = useSitzung()
  const navigate = useNavigate()
  const ort = useLocation()
  const ziel = (ort.state as { von?: string } | null)?.von ?? '/projekte'

  const [benutzername, setBenutzername] = useState('')
  const [passwort, setPasswort] = useState('')
  const [fehler, setFehler] = useState<string | null>(null)
  const [sendet, setSendet] = useState(false)

  // Schon angemeldet (z. B. Zurück-Taste): direkt weiter.
  if (benutzer) {
    return <Navigate to={ziel} replace />
  }

  async function absenden(ereignis: FormEvent) {
    ereignis.preventDefault()
    setSendet(true)
    setFehler(null)
    try {
      await anmelden(benutzername, passwort)
      navigate(ziel, { replace: true })
    } catch (e) {
      setFehler(
        e instanceof ApiFehler && e.status === 401
          ? 'Benutzername oder Passwort falsch'
          : 'Anmeldung zurzeit nicht möglich. Bitte später erneut versuchen.',
      )
    } finally {
      setSendet(false)
    }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 10, px: 2 }}>
      <Typography variant="h4" component="div" sx={{ mb: 4 }}>
        Aufgabenmanager
      </Typography>
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent component="form" onSubmit={absenden} noValidate sx={{ display: 'grid', gap: 2, p: 4 }}>
          <Typography variant="h5" component="h1">
            Anmelden
          </Typography>
          <TextField
            label="Benutzername"
            value={benutzername}
            onChange={(e) => setBenutzername(e.target.value)}
            autoComplete="username"
            autoFocus
            required
          />
          <TextField
            label="Passwort"
            type="password"
            value={passwort}
            onChange={(e) => setPasswort(e.target.value)}
            autoComplete="current-password"
            required
          />
          {fehler && <Alert severity="error">{fehler}</Alert>}
          <Button type="submit" variant="contained" size="large" disabled={sendet || !benutzername || !passwort}>
            Anmelden
          </Button>
        </CardContent>
      </Card>
    </Box>
  )
}

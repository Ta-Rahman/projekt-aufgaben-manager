import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { NavLink, Outlet } from 'react-router'
import { useSitzung } from '../auth/Sitzung.tsx'

// Gemeinsamer Rahmen aller Seiten nach dem Login (App-Leiste aus den Wireframes wf_02–wf_06):
// Titel, „Projekte“, „Benutzer“ (nur ADMIN), angemeldete Person mit Rolle, „Abmelden“.

const navLinkStil = {
  color: 'inherit',
  '&.active': { textDecoration: 'underline', textUnderlineOffset: '6px' },
}

export default function AppLayout() {
  // Nach dem Abmelden leitet GeschuetzteRoute zur Login-Seite (ohne gemerkte Zieladresse).
  const { benutzer, abmelden } = useSitzung()

  return (
    <>
      <AppBar position="static">
        <Toolbar sx={{ gap: 1 }}>
          <Typography variant="h6" component="div" sx={{ mr: 3 }}>
            Aufgabenmanager
          </Typography>
          <Button component={NavLink} to="/projekte" sx={navLinkStil}>
            Projekte
          </Button>
          {benutzer?.rolle === 'ADMIN' && (
            <Button component={NavLink} to="/benutzer" sx={navLinkStil}>
              Benutzer
            </Button>
          )}
          <Box sx={{ flexGrow: 1 }} />
          {benutzer && (
            <Typography variant="body2" sx={{ mr: 2 }}>
              {benutzer.anzeigename} ({benutzer.rolle})
            </Typography>
          )}
          <Button color="inherit" variant="outlined" onClick={abmelden}>
            Abmelden
          </Button>
        </Toolbar>
      </AppBar>
      <Container component="main" maxWidth="lg" sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </>
  )
}

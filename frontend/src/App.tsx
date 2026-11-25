import { Typography } from '@mui/material'
import { Navigate, Route, Routes } from 'react-router'
import GeschuetzteRoute from './auth/GeschuetzteRoute.tsx'
import AppLayout from './layout/AppLayout.tsx'
import LoginSeite from './pages/LoginSeite.tsx'
import NichtGefunden from './pages/NichtGefunden.tsx'

// Routen laut entwurf/Seiten.md. Alles außer /login liegt hinter GeschuetzteRoute und im AppLayout
// (verschachtelte Route: AppLayout zeichnet die App-Leiste, <Outlet /> darin die jeweilige Seite).
// Die Seiten Projektübersicht, Projektdetail und Benutzerverwaltung folgen im nächsten Abschnitt.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginSeite />} />
      <Route
        element={
          <GeschuetzteRoute>
            <AppLayout />
          </GeschuetzteRoute>
        }
      >
        <Route index element={<Navigate to="/projekte" replace />} />
        <Route path="projekte" element={<Platzhalter titel="Projekte" />} />
        <Route path="projekte/:id" element={<Platzhalter titel="Projekt" />} />
        <Route
          path="benutzer"
          element={
            <GeschuetzteRoute rollen={['ADMIN']}>
              <Platzhalter titel="Benutzer" />
            </GeschuetzteRoute>
          }
        />
        <Route path="*" element={<NichtGefunden />} />
      </Route>
    </Routes>
  )
}

/** Vorläufig, bis die Seiten umgesetzt sind. */
function Platzhalter({ titel }: { titel: string }) {
  return (
    <Typography variant="h4" component="h1">
      {titel}
    </Typography>
  )
}

import { Navigate, Route, Routes } from 'react-router'
import GeschuetzteRoute from './auth/GeschuetzteRoute.tsx'
import BenutzerSeite from './benutzer/BenutzerSeite.tsx'
import AppLayout from './layout/AppLayout.tsx'
import LoginSeite from './pages/LoginSeite.tsx'
import NichtGefunden from './pages/NichtGefunden.tsx'
import ProjektDetailSeite from './projekte/ProjektDetailSeite.tsx'
import ProjektUebersicht from './projekte/ProjektUebersicht.tsx'

// Routen laut entwurf/Seiten.md. Alles außer /login liegt hinter GeschuetzteRoute und im AppLayout
// (verschachtelte Route: AppLayout zeichnet die App-Leiste, <Outlet /> darin die jeweilige Seite).
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
        <Route path="projekte" element={<ProjektUebersicht />} />
        <Route path="projekte/:id" element={<ProjektDetailSeite />} />
        <Route
          path="benutzer"
          element={
            <GeschuetzteRoute rollen={['ADMIN']}>
              <BenutzerSeite />
            </GeschuetzteRoute>
          }
        />
        <Route path="*" element={<NichtGefunden />} />
      </Route>
    </Routes>
  )
}

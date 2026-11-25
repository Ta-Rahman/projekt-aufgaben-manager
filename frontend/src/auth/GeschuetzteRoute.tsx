import { Box, CircularProgress } from '@mui/material'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { Rolle } from '../api/typen.ts'
import NichtGefunden from '../pages/NichtGefunden.tsx'
import { useSitzung } from './Sitzung.tsx'

type Props = {
  children: ReactNode
  /** Nur für diese Rollen; alle anderen sehen „Seite nicht gefunden“ (wie 404 im Backend, US8). */
  rollen?: Rolle[]
}

/**
 * Lässt nur angemeldete Benutzer durch. Ohne Sitzung geht es zur Login-Seite; die gewünschte Adresse
 * wird mitgegeben, damit der Login danach dorthin zurückführt.
 * Das ist nur Benutzerführung: Geschützt sind die Daten durch das Backend, nicht durch diese Komponente.
 */
export default function GeschuetzteRoute({ children, rollen }: Props) {
  const { benutzer } = useSitzung()
  const ort = useLocation()

  if (benutzer === undefined) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress aria-label="Lädt" />
      </Box>
    )
  }
  if (benutzer === null) {
    return <Navigate to="/login" replace state={{ von: ort.pathname }} />
  }
  if (rollen && !rollen.includes(benutzer.rolle)) {
    return <NichtGefunden />
  }
  return children
}

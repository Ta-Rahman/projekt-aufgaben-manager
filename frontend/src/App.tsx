import { Route, Routes } from 'react-router'
import StartSeite from './pages/StartSeite.tsx'
import NichtGefunden from './pages/NichtGefunden.tsx'

// Routen-Gerüst. Die fachlichen Seiten kommen nach dem Entwurf (Wireframes, Navigation) dazu.
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<StartSeite />} />
      <Route path="*" element={<NichtGefunden />} />
    </Routes>
  )
}

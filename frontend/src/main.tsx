import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { CssBaseline, ThemeProvider } from '@mui/material'
import App from './App.tsx'
import { SitzungProvider } from './auth/Sitzung.tsx'
import { theme } from './theme.ts'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SitzungProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </SitzungProvider>
    </ThemeProvider>
  </StrictMode>,
)

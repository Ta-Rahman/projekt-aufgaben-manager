import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'

function renderMitRoute(pfad: string) {
  return render(
    <MemoryRouter initialEntries={[pfad]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App-Routing', () => {
  it('zeigt die Startseite unter /', () => {
    renderMitRoute('/')
    expect(screen.getByRole('heading', { name: 'Aufgaben-Manager' })).toBeInTheDocument()
  })

  it('zeigt bei unbekanntem Pfad die 404-Seite und führt zurück zur Startseite', async () => {
    renderMitRoute('/gibt-es-nicht')
    expect(screen.getByRole('heading', { name: 'Seite nicht gefunden' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('link', { name: 'Zur Startseite' }))
    expect(screen.getByRole('heading', { name: 'Aufgaben-Manager' })).toBeInTheDocument()
  })
})

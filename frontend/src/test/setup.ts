// Ergänzt expect() um DOM-Matcher wie toBeInTheDocument().
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// DOM nach jedem Test leeren (ohne globals: true nötig).
afterEach(() => {
  cleanup()
})

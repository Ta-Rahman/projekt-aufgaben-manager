import { expect, type Page } from '@playwright/test'

/** Meldet einen Seed-Benutzer an (Passwort aller Demo-Benutzer: „demo“, siehe README). */
export async function anmelden(page: Page, benutzername: string) {
  await page.goto('/login')
  await page.getByLabel('Benutzername').fill(benutzername)
  await page.getByLabel('Passwort').fill('demo')
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByRole('heading', { name: 'Projekte' })).toBeVisible()
}

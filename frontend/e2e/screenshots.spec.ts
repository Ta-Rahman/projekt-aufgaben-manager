import { expect, test, type Page } from '@playwright/test'
import path from 'node:path'
import { anmelden } from './hilfen.ts'

// Screenshots aller Seiten für die Dokumentation, in derselben Reihenfolge und Nummerierung wie die
// Wireframes (wf_01 … wf_06). Kein Test im eigentlichen Sinn; läuft nur mit `npm run screenshots`
// gegen ein frisch gestartetes Backend mit Seed-Daten. Die Bilder gehören nicht ins Repository.

const ZIEL = process.env.SCREENSHOT_VERZEICHNIS ?? path.resolve('..', '..', 'screenshots')

async function foto(page: Page, name: string) {
  // Animationen (Dialog-Einblendung, Fortschrittsbalken) abwarten.
  await page.waitForTimeout(400)
  await page.screenshot({ path: path.join(ZIEL, `${name}.png`) })
}

test.describe.configure({ mode: 'serial' })

test('sc_01 Login mit Fehlermeldung', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Benutzername').fill('admin')
  await page.getByLabel('Passwort').fill('falsch')
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByText('Benutzername oder Passwort falsch')).toBeVisible()
  await foto(page, 'sc_01_login')
})

test('sc_02 bis sc_06 als Administration', async ({ page }) => {
  await anmelden(page, 'admin')
  await expect(page.getByRole('link', { name: 'Relaunch Kundenportal' })).toBeVisible()
  await foto(page, 'sc_02_projektuebersicht')

  await page.getByRole('link', { name: 'Relaunch Kundenportal' }).click()
  await expect(page.getByRole('region', { name: 'Offen' })).toBeVisible()
  await foto(page, 'sc_03_projektdetail_aufgaben')

  await page.getByRole('tab', { name: /Mitglieder/ }).click()
  await expect(page.getByRole('table', { name: 'Mitglieder' })).toBeVisible()
  await foto(page, 'sc_04_projektdetail_mitglieder')

  await page.getByRole('tab', { name: 'Aufgaben' }).click()
  await page.getByText('Login an Kunden-SSO anbinden').click()
  await expect(page.getByRole('heading', { name: 'Aufgabe bearbeiten' })).toBeVisible()
  await foto(page, 'sc_06_aufgabendialog')
  await page.getByRole('button', { name: 'Abbrechen' }).click()

  await page.getByRole('link', { name: 'Benutzer' }).click()
  await expect(page.getByRole('cell', { name: 'Erik Ehemalig' })).toBeVisible()
  await foto(page, 'sc_05_benutzerverwaltung')
})

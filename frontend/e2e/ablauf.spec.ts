import { expect, test } from '@playwright/test'
import { anmelden } from './hilfen.ts'

// Durchgehender Ablauf laut Arbeitsplan (Abschnitt 5, E2E): Login → Projekt anlegen → Aufgabe anlegen →
// Status erledigt → Fortschritt 100 %. Läuft gegen echtes Frontend, Backend und PostgreSQL.

test('Projektleitung legt Projekt und Aufgabe an und schließt die Aufgabe ab', async ({ page }) => {
  // Eindeutiger Name, falls das Backend aus einem früheren Lauf wiederverwendet wird.
  const projekt = `E2E-Projekt ${Date.now()}`

  await anmelden(page, 'leitung')

  // US2: Projekt anlegen
  await page.getByRole('button', { name: /Neues Projekt/ }).click()
  await page.getByLabel('Name').fill(projekt)
  await page.getByRole('button', { name: 'Speichern' }).click()
  await expect(page.getByRole('heading', { name: projekt })).toBeVisible()
  await expect(page.getByText('0 von 0 Aufgaben erledigt (0 %)')).toBeVisible()

  // US4: Aufgabe anlegen, Leitung als zuständig
  await page.getByRole('button', { name: /Neue Aufgabe/ }).click()
  await page.getByLabel('Titel').fill('Testfall schreiben')
  await page.getByRole('combobox', { name: 'Zuständig' }).click()
  await page.getByRole('option', { name: 'Lukas Leitung' }).click()
  await page.getByRole('button', { name: 'Speichern' }).click()

  const offen = page.getByRole('region', { name: 'Offen' })
  await expect(offen.getByText('Testfall schreiben')).toBeVisible()
  await expect(offen.getByText('Offen (1)')).toBeVisible()

  // US5: Status ändern, US6: Fortschritt
  await page.getByRole('combobox', { name: 'Status von Testfall schreiben' }).click()
  await page.getByRole('option', { name: 'Erledigt' }).click()
  await expect(page.getByRole('region', { name: 'Erledigt' }).getByText('Testfall schreiben')).toBeVisible()
  await expect(page.getByText('1 von 1 Aufgaben erledigt (100 %)')).toBeVisible()

  // Übersicht zeigt denselben Fortschritt
  await page.getByRole('link', { name: '← Projekte' }).click()
  const zeile = page.getByRole('row').filter({ hasText: projekt })
  await expect(zeile.getByText('100 %')).toBeVisible()
  await expect(zeile.getByText('1 / 1')).toBeVisible()
})

test('US8: fremdes Projekt bleibt unsichtbar, auch über die Adresse', async ({ page }) => {
  // mitarbeiter2 ist nur im Mailserver-Projekt.
  await anmelden(page, 'mitarbeiter2')
  await expect(page.getByRole('link', { name: 'Migration Mailserver' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Relaunch Kundenportal' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Abmelden' }).click()

  // Kundenportal über die Adresse aufrufen: dieselbe Seite wie bei einer unbekannten ID.
  await anmelden(page, 'leitung')
  await page.getByRole('link', { name: 'Relaunch Kundenportal' }).click()
  const adresse = page.url()
  await page.getByRole('button', { name: 'Abmelden' }).click()
  await anmelden(page, 'mitarbeiter2')
  await page.goto(adresse)
  await expect(page.getByRole('heading', { name: 'Seite nicht gefunden' })).toBeVisible()
})

test('US7: falsches Passwort und deaktivierter Benutzer werden abgewiesen', async ({ page }) => {
  for (const [benutzer, passwort] of [
    ['leitung', 'falsch'],
    ['ehemalig', 'demo'],
  ]) {
    await page.goto('/login')
    await page.getByLabel('Benutzername').fill(benutzer)
    await page.getByLabel('Passwort').fill(passwort)
    await page.getByRole('button', { name: 'Anmelden' }).click()
    await expect(page.getByText('Benutzername oder Passwort falsch')).toBeVisible()
  }
})

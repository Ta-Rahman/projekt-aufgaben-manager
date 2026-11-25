import { defineConfig, devices } from '@playwright/test'

// End-to-End-Tests mit Playwright: echter Browser gegen echtes Frontend (Vite) und echtes Backend.
//
// Das Backend startet über `mvnw spring-boot:test-run` mit einer frischen PostgreSQL aus Testcontainers
// (Seed-Daten aus Flyway V2). Docker muss laufen.
//
//   npm run e2e           fachlicher Ablauf (Projekt → Aufgabe → erledigt → Fortschritt) und Rechte
//   npm run screenshots   Screenshots der Seiten für die Dokumentation, nicht Teil der Tests;
//                         Ablage außerhalb des Repositorys (Standard: ../../screenshots, änderbar per
//                         Umgebungsvariable SCREENSHOT_VERZEICHNIS)
//
// Lokal wird der installierte Microsoft Edge verwendet (kein Browser-Download nötig).

const screenshotModus = process.argv.some((a) => a.includes('screenshots'))
const windows = process.platform === 'win32'

export default defineConfig({
  testDir: './e2e',
  // Die Tests teilen sich ein Backend mit einer Datenbank, deshalb nacheinander.
  workers: 1,
  fullyParallel: false,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    channel: process.env.CI ? undefined : 'msedge',
    locale: 'de-DE',
    trace: 'off',
  },
  projects: [
    { name: 'e2e', testIgnore: /screenshots\.spec\.ts/, use: { ...devices['Desktop Edge'] } },
    {
      name: 'screenshots',
      testMatch: /screenshots\.spec\.ts/,
      // Seitenverhältnis wie die Wireframes, doppelte Pixeldichte für den Druck.
      // --lang: Datumsfelder im deutschen Format (folgen der Sprache der Browseroberfläche, nicht `locale`).
      use: {
        viewport: { width: 1440, height: 900 },
        deviceScaleFactor: 2,
        launchOptions: { args: ['--lang=de-DE'] },
      },
    },
  ],
  webServer: [
    {
      // Unter Windows mit ausdrücklichem Pfad: cmd.exe fand mvnw.cmd im Arbeitsverzeichnis sonst nicht.
      command: windows ? '.\\mvnw.cmd -B -q spring-boot:test-run' : './mvnw -B -q spring-boot:test-run',
      cwd: '../backend',
      url: 'http://localhost:8080/api/auth/csrf',
      timeout: 180_000,
      // Für Screenshots immer ein frisch gestartetes Backend (unveränderte Seed-Daten); läuft schon eines,
      // bricht Playwright mit „Port belegt“ ab, statt veraltete Daten zu fotografieren.
      reuseExistingServer: !screenshotModus,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: true,
    },
  ],
})

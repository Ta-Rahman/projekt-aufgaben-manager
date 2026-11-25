# Projekt- und Aufgabenmanager

Webanwendung für Projekt- und Aufgabenmanagement in einem IT-Dienstleistungsunternehmen
(Fallstudie). Backend mit Spring Boot und Spring Data JPA, Frontend mit React.

Funktionen: Anmeldung mit Rollen (Administration, Projektleitung, Mitarbeit), Benutzerverwaltung,
Projekte anlegen, bearbeiten und archivieren, Projektmitglieder, Aufgaben mit Status in einem Board,
Fortschritt je Projekt. Jede Person sieht nur die Projekte, die sie leitet oder in denen sie Mitglied ist.

## Aufbau

| Ordner / Datei | Inhalt |
|---|---|
| `backend/` | Spring Boot 4 (Java 21, Maven Wrapper): REST-API, Spring Security, JPA, Flyway |
| `frontend/` | Vite + React + TypeScript, React Router, MUI; Playwright-Tests in `frontend/e2e/` |
| `docker-compose.yml` | PostgreSQL 18 für die lokale Entwicklung |
| `.github/workflows/ci.yml` | CI: Backend- und Frontend-Tests bei jedem Push |

## Voraussetzungen

- JDK 21
- Docker mit Docker Compose (für die Datenbank und für die Tests)
- Node.js 22.22 oder neuer (die CI nutzt Node 24 LTS)

## Starten im Entwicklungsmodus

Backend (startet die PostgreSQL-Datenbank aus `docker-compose.yml` automatisch mit):

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Frontend (in einem zweiten Terminal):

```bash
cd frontend
npm install
npm run dev
```

Die Anwendung läuft dann unter <http://localhost:5173>; Aufrufe unter `/api` leitet Vite an das
Backend auf Port 8080 weiter. Änderungen am Frontend erscheinen sofort im Browser.

Die Datenbank ist auf `localhost:5434` erreichbar (Port per Umgebungsvariable `DB_PORT` änderbar)
und läuft nach dem Beenden des Backends weiter. Stoppen mit `docker compose down`.

Alternativ startet `./mvnw spring-boot:test-run` das Backend mit einer frischen, leeren
Datenbank aus Testcontainers (nur Beispieldaten, nichts bleibt erhalten).

## Als eine Anwendung bauen und starten

Spring Boot liefert das gebaute Frontend selbst aus; dann reicht ein Prozess auf Port 8080.
Reihenfolge beachten: erst das Frontend bauen, dann das Backend packen.

```bash
cd frontend && npm install && npm run build      # erzeugt frontend/dist
cd ../backend && ./mvnw package                  # kopiert frontend/dist ins Jar (static/)
```

Das Jar enthält bewusst keinen Docker-Compose-Start. Die Datenbank kommt per Umgebungsvariablen,
zum Beispiel für die Compose-Datenbank (vorher `docker compose up -d` im Hauptordner):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/aufgaben_manager \
SPRING_DATASOURCE_USERNAME=demo_user \
SPRING_DATASOURCE_PASSWORD=demo_passwort_nur_lokal \
java -jar backend/target/aufgaben-manager-0.0.1-SNAPSHOT.jar
```

Die Anwendung läuft dann unter <http://localhost:8080>.

## Demo-Zugänge

Beim ersten Start legt die Migration `V2__seed.sql` eine Beispielorganisation mit Projekten,
Aufgaben und folgenden Benutzern an. Das Passwort ist bei allen `demo`.

| Benutzer | Rolle | Bemerkung |
|---|---|---|
| `admin` | Administration | sieht alle Projekte, verwaltet Benutzer |
| `leitung` | Projektleitung | leitet „Relaunch Kundenportal“ und das archivierte Projekt |
| `leitung2` | Projektleitung | leitet „Migration Mailserver“ |
| `mitarbeiter` | Mitarbeit | Mitglied in allen drei Projekten |
| `mitarbeiter2` | Mitarbeit | nur Mitglied in „Migration Mailserver“ |
| `ehemalig` | Mitarbeit | deaktiviert, Anmeldung nicht möglich |

Die Datenbank-Zugangsdaten in `docker-compose.yml` und die Demo-Passwörter sind reine Demo-Werte
für die lokale Entwicklung.

## Tests

```bash
cd backend && ./mvnw verify      # Unit-Tests (Mockito), API- und Integrationstests gegen PostgreSQL (Testcontainers)
cd frontend && npm test          # Vitest + React Testing Library
cd frontend && npm run build     # Typprüfung und Produktions-Build
cd frontend && npm run e2e       # Playwright: Ende-zu-Ende im Browser (nur lokal, nicht in der CI)
```

Für die Backend-Tests und die E2E-Tests muss Docker laufen; Testcontainers startet dafür einen
eigenen PostgreSQL-Container.

Die E2E-Tests starten Backend (`spring-boot:test-run`) und Frontend selbst und nutzen den
installierten Microsoft Edge. Ohne Edge: in `frontend/playwright.config.ts` den Eintrag `channel`
entfernen und einmalig `npx playwright install chromium` ausführen.

`npm run screenshots` erzeugt Bildschirmfotos aller Seiten für die Dokumentation. Sie werden
außerhalb des Repositorys abgelegt (Standard `../../screenshots`, änderbar über die
Umgebungsvariable `SCREENSHOT_VERZEICHNIS`).

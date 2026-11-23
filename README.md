# Projekt- und Aufgabenmanager

Webanwendung für Projekt- und Aufgabenmanagement in einem IT-Dienstleistungsunternehmen
(Fallstudie). Backend mit Spring Boot und Spring Data JPA, Frontend mit React.

> Stand: technisches Grundgerüst. Fachfunktionen folgen.

## Aufbau

| Ordner / Datei | Inhalt |
|---|---|
| `backend/` | Spring Boot 4 (Java 21, Maven Wrapper): REST-API, Spring Security, JPA, Flyway |
| `frontend/` | Vite + React + TypeScript, React Router, MUI |
| `docker-compose.yml` | PostgreSQL 18 für die lokale Entwicklung |
| `.github/workflows/ci.yml` | CI: Backend- und Frontend-Tests bei jedem Push |

## Voraussetzungen

- JDK 21
- Docker mit Docker Compose (für die Datenbank und für die Tests)
- Node.js 22.22 oder neuer (die CI nutzt Node 24 LTS)

## Starten

Backend (startet die PostgreSQL-Datenbank aus `docker-compose.yml` automatisch mit):

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Frontend im Entwicklungsmodus (in einem zweiten Terminal):

```bash
cd frontend
npm install
npm run dev
```

Die Anwendung läuft dann unter <http://localhost:5173>; Aufrufe unter `/api` leitet Vite an das
Backend auf Port 8080 weiter.

Die Datenbank ist auf `localhost:5434` erreichbar (Port per Umgebungsvariable `DB_PORT` änderbar)
und läuft nach dem Beenden des Backends weiter. Stoppen mit `docker compose down`.

### Demo-Zugang (vorläufig)

| Benutzer | Passwort |
|---|---|
| `demo` | `demo` |

Die Datenbank-Zugangsdaten in `docker-compose.yml` und der Demo-Benutzer sind reine Demo-Werte für
die lokale Entwicklung. Der Demo-Benutzer wird durch eine Benutzerverwaltung ersetzt.

## Tests

```bash
cd backend && ./mvnw verify      # JUnit, Spring-Security-Tests, Integrationstest gegen PostgreSQL (Testcontainers)
cd frontend && npm test          # Vitest + React Testing Library
cd frontend && npm run build     # Typprüfung und Produktions-Build
```

Für die Backend-Tests muss Docker laufen; Testcontainers startet dafür einen eigenen
PostgreSQL-Container.

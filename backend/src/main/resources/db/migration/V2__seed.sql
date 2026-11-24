-- V2: Beispieldaten für Entwicklung, Vorführung und Screenshots.
-- Alle Passwörter sind reine DEMO-Werte: Klartext "demo", gespeichert als BCrypt-Hash mit Präfix {bcrypt}
-- (DelegatingPasswordEncoder). Für einen echten Betrieb müsste diese Migration entfallen oder die
-- Passwörter müssten nach dem ersten Start geändert werden.
-- IDs vergibt PostgreSQL (identity), Verweise deshalb über Unterabfragen auf eindeutige Spalten.

INSERT INTO organisation (name) VALUES ('Nordlicht IT-Services GmbH');

INSERT INTO app_user (organisation_id, username, display_name, password_hash, role, active)
SELECT o.id, u.username, u.display_name,
       '{bcrypt}$2a$10$2FS/0M9hflDtFVds4k971unXVLBOYrtnByxdlQBvzvlplZPTzLzPG',
       u.role, u.active
FROM organisation o,
     (VALUES ('admin',     'Anna Admin',       'ADMIN',         true),
             ('leitung',   'Lukas Leitung',    'PROJEKTLEITER', true),
             ('leitung2',  'Petra Planer',     'PROJEKTLEITER', true),
             ('mitarbeiter',  'Mia Mitarbeiter',  'MITARBEITER', true),
             ('mitarbeiter2', 'Tom Tester',       'MITARBEITER', true),
             ('ehemalig',  'Erik Ehemalig',    'MITARBEITER',   false)
     ) AS u (username, display_name, role, active)
WHERE o.name = 'Nordlicht IT-Services GmbH';

-- Drei Projekte: zwei aktive mit unterschiedlicher Leitung, eines archiviert.
INSERT INTO project (organisation_id, lead_id, name, description, start_date, end_date, archived_at)
SELECT o.id, l.id, p.name, p.description, p.start_date, p.end_date, p.archived_at
FROM organisation o,
     (VALUES ('leitung',  'Relaunch Kundenportal', 'Neues Kundenportal für einen Versicherungskunden', DATE '2026-11-02', DATE '2027-01-29', NULL::timestamptz),
             ('leitung2', 'Migration Mailserver',  'Umzug der Postfächer auf die neue Plattform',      DATE '2026-11-16', DATE '2026-12-18', NULL::timestamptz),
             ('leitung',  'Inventur Hardware 2025', 'Bestandsaufnahme aller Arbeitsplatzrechner',      DATE '2025-10-01', DATE '2025-12-19', TIMESTAMPTZ '2026-01-15 10:00:00+01')
     ) AS p (lead, name, description, start_date, end_date, archived_at)
JOIN app_user l ON l.username = p.lead
WHERE o.name = 'Nordlicht IT-Services GmbH';

-- Mitgliedschaften. Die Leitung ist nicht automatisch Mitglied (D2).
-- mitarbeiter2 ist nur im Mailserver-Projekt: sieht das Kundenportal also nicht (US8).
INSERT INTO project_member (project_id, user_id)
SELECT p.id, u.id
FROM (VALUES ('Relaunch Kundenportal',  'mitarbeiter'),
             ('Relaunch Kundenportal',  'ehemalig'),
             ('Migration Mailserver',   'mitarbeiter'),
             ('Migration Mailserver',   'mitarbeiter2'),
             ('Inventur Hardware 2025', 'mitarbeiter')
     ) AS m (project, username)
JOIN project p ON p.name = m.project
JOIN app_user u ON u.username = m.username;

INSERT INTO task (project_id, created_by, assignee_id, title, description, status, due_date)
SELECT p.id, c.id, a.id, t.title, t.description, t.status, t.due_date
FROM (VALUES
        ('Relaunch Kundenportal', 'leitung', 'mitarbeiter', 'Anforderungen mit Fachbereich abstimmen', NULL, 'ERLEDIGT', DATE '2026-11-13'),
        ('Relaunch Kundenportal', 'leitung', 'mitarbeiter', 'Wireframes Startseite', 'Desktop und Mobil', 'ERLEDIGT', DATE '2026-11-20'),
        ('Relaunch Kundenportal', 'leitung', 'mitarbeiter', 'Login an Kunden-SSO anbinden', NULL, 'IN_BEARBEITUNG', DATE '2026-12-04'),
        ('Relaunch Kundenportal', 'mitarbeiter', 'ehemalig', 'Altdaten der Kundenkonten prüfen', 'Zuständigkeit noch beim ausgeschiedenen Kollegen (D6)', 'OFFEN', DATE '2026-12-11'),
        ('Relaunch Kundenportal', 'leitung', NULL, 'Lasttest vorbereiten', NULL, 'OFFEN', NULL),
        ('Migration Mailserver', 'leitung2', 'mitarbeiter2', 'Postfachgrößen erheben', NULL, 'ERLEDIGT', DATE '2026-11-20'),
        ('Migration Mailserver', 'leitung2', 'mitarbeiter', 'Testmigration mit fünf Postfächern', NULL, 'IN_BEARBEITUNG', DATE '2026-12-02'),
        ('Migration Mailserver', 'leitung2', 'mitarbeiter2', 'Benutzer informieren', 'Rundmail und Intranet-Hinweis', 'OFFEN', DATE '2026-12-09'),
        ('Inventur Hardware 2025', 'leitung', 'mitarbeiter', 'Geräteliste exportieren', NULL, 'ERLEDIGT', DATE '2025-10-15'),
        ('Inventur Hardware 2025', 'leitung', 'mitarbeiter', 'Abgleich mit Buchhaltung', NULL, 'ERLEDIGT', DATE '2025-12-12')
     ) AS t (project, creator, assignee, title, description, status, due_date)
JOIN project p ON p.name = t.project
JOIN app_user c ON c.username = t.creator
LEFT JOIN app_user a ON a.username = t.assignee;

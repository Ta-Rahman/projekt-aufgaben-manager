-- V1: Grundschema Projekt- und Aufgabenmanagement.
-- Flyway führt jede Datei genau einmal aus und merkt sich das in flyway_schema_history.
-- Änderungen am Schema kommen deshalb immer als neue Datei (V2, V3, ...), nie als Änderung an dieser.

-- Mandant. Im Betrieb gibt es vorerst genau einen; organisation_id an Benutzer und Projekt
-- bereitet die Mandantenfähigkeit vor.
CREATE TABLE organisation (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        varchar(200) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now()
);

-- "user" ist in PostgreSQL ein reserviertes Wort, daher app_user.
CREATE TABLE app_user (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organisation_id  bigint       NOT NULL REFERENCES organisation (id),
    username         varchar(50)  NOT NULL,
    display_name     varchar(100) NOT NULL,
    password_hash    varchar(100) NOT NULL,
    role             varchar(20)  NOT NULL,
    active           boolean      NOT NULL DEFAULT true,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'PROJEKTLEITER', 'MITARBEITER'))
);

CREATE TABLE project (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organisation_id  bigint       NOT NULL REFERENCES organisation (id),
    lead_id          bigint       NOT NULL REFERENCES app_user (id),
    name             varchar(200) NOT NULL,
    description      text,
    start_date       date,
    end_date         date,
    archived_at      timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    version          bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_zeitraum CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

-- Mitgliedschaft (n:m) ohne Zusatzspalten; in JPA keine eigene Entität, sondern @JoinTable an Project.members.
CREATE TABLE project_member (
    project_id  bigint NOT NULL REFERENCES project (id),
    user_id     bigint NOT NULL REFERENCES app_user (id),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE task (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id   bigint       NOT NULL REFERENCES project (id),
    created_by   bigint       NOT NULL REFERENCES app_user (id),
    assignee_id  bigint       REFERENCES app_user (id),
    title        varchar(200) NOT NULL,
    description  text,
    status       varchar(20)  NOT NULL DEFAULT 'OFFEN',
    due_date     date,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    version      bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_task_status CHECK (status IN ('OFFEN', 'IN_BEARBEITUNG', 'ERLEDIGT'))
);

-- Indizes für die häufigen Abfragen: Aufgaben je Projekt und Status (Board, Fortschritt),
-- Projekte eines Mitglieds (Sichtbarkeit US8), aktive Projekte je Organisation.
-- Fremdschlüssel bekommen in PostgreSQL keinen automatischen Index.
CREATE INDEX ix_task_project_status      ON task (project_id, status);
CREATE INDEX ix_task_assignee            ON task (assignee_id);
CREATE INDEX ix_project_member_user      ON project_member (user_id);
CREATE INDEX ix_project_org_archived     ON project (organisation_id, archived_at);
CREATE INDEX ix_project_lead             ON project (lead_id);

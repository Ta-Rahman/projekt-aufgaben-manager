package io.github.tarahman.aufgabenmanager.project;

import org.springframework.data.jpa.repository.JpaRepository;

/** Abfragen für Sichtbarkeit (US8) und Fortschritt (US6) kommen mit den jeweiligen Stories dazu. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}

package io.github.tarahman.aufgabenmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.project.Project;
import io.github.tarahman.aufgabenmanager.project.ProjectRepository;
import io.github.tarahman.aufgabenmanager.task.Task;
import io.github.tarahman.aufgabenmanager.task.TaskRepository;
import io.github.tarahman.aufgabenmanager.task.TaskStatus;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Prüft das Zusammenspiel von Flyway-Schema (V1) und JPA-Entitäten gegen echtes PostgreSQL.
 *
 * <p>{@code @DataJpaTest} lädt nur die Persistenzschicht (Entitäten, Repositories, Flyway), keine Controller
 * und keine Security. Jeder Test läuft in einer Transaktion, die am Ende zurückgerollt wird, daher sehen sich
 * die Tests gegenseitig nicht. Dass der Kontext überhaupt startet, belegt bereits, dass
 * {@code ddl-auto=validate} Entitäten und Tabellen als passend erkannt hat.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class DatenmodellTest {

	@Autowired
	TestEntityManager em;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	TaskRepository taskRepository;

	@Autowired
	JdbcTemplate jdbc;

	Organisation organisation;

	AppUser leitung;

	AppUser mitarbeiterin;

	@BeforeEach
	void grunddaten() {
		organisation = em.persist(new Organisation("Testfirma"));
		leitung = em.persist(new AppUser(organisation, "pl", "Paula Leitung", "{noop}x", Role.PROJEKTLEITER));
		mitarbeiterin = em.persist(new AppUser(organisation, "ma", "Max Arbeit", "{noop}x", Role.MITARBEITER));
	}

	@Test
	void projektMitMitgliedUndAufgabeWirdGespeichertUndNeuGeladen() {
		Project projekt = new Project(organisation, leitung, "Relaunch Website");
		projekt.setStartDate(LocalDate.of(2026, 12, 1));
		projekt.getMembers().add(mitarbeiterin);
		em.persist(projekt);
		Task aufgabe = new Task(projekt, leitung, "Startseite entwerfen");
		aufgabe.setAssignee(mitarbeiterin);
		em.persist(aufgabe);

		// flush schreibt alle ausstehenden INSERTs in die Datenbank, clear leert den Persistenzkontext
		// (den Cache der Sitzung). Danach kommt jedes Objekt garantiert frisch aus PostgreSQL.
		em.flush();
		em.clear();

		Project geladen = projectRepository.findById(projekt.getId()).orElseThrow();
		assertThat(geladen.getName()).isEqualTo("Relaunch Website");
		assertThat(geladen.getMembers()).extracting(AppUser::getUsername).containsExactly("ma");
		assertThat(geladen.getVersion()).isZero();
		assertThat(geladen.getCreatedAt()).isNotNull().isEqualTo(geladen.getUpdatedAt());
		assertThat(geladen.isArchived()).isFalse();

		Task geladeneAufgabe = taskRepository.findByProjectIdOrderByIdAsc(projekt.getId()).getFirst();
		assertThat(geladeneAufgabe.getStatus()).isEqualTo(TaskStatus.OFFEN);
		assertThat(geladeneAufgabe.getAssignee().getUsername()).isEqualTo("ma");
	}

	@Test
	void manyToOneWirdErstBeimZugriffGeladen() {
		Project projekt = em.persist(new Project(organisation, leitung, "Lazy"));
		Task aufgabe = em.persist(new Task(projekt, leitung, "Aufgabe"));
		em.flush();
		em.clear();

		Task geladen = taskRepository.findById(aufgabe.getId()).orElseThrow();

		// FetchType.LAZY: Statt des Projekts steckt zunächst ein Platzhalter (Proxy) im Feld, der nur die ID kennt.
		assertThat(Hibernate.isInitialized(geladen.getProject())).isFalse();
		assertThat(geladen.getProject().getId()).isEqualTo(projekt.getId());
		assertThat(Hibernate.isInitialized(geladen.getProject())).isFalse();
		// Erst der Zugriff auf ein anderes Feld löst das SELECT auf project aus.
		assertThat(geladen.getProject().getName()).isEqualTo("Lazy");
		assertThat(Hibernate.isInitialized(geladen.getProject())).isTrue();
	}

	@Test
	void aenderungErhoehtVersionUndUpdatedAt() {
		Project projekt = em.persistFlushFind(new Project(organisation, leitung, "Alt"));
		var vorher = projekt.getUpdatedAt();

		projekt.setName("Neu");
		em.flush();

		assertThat(projekt.getVersion()).isEqualTo(1L);
		assertThat(projekt.getUpdatedAt()).isAfterOrEqualTo(vorher);
	}

	@Test
	void veralteterStandWirdBeimSpeichernAbgelehnt() {
		Project projekt = em.persistFlushFind(new Project(organisation, leitung, "Original"));
		Long id = projekt.getId();
		em.detach(projekt); // Stand von Benutzer A, Version 0, außerhalb des Persistenzkontexts

		Project vonB = em.find(Project.class, id);
		vonB.setName("Änderung B");
		em.flush(); // B speichert zuerst: Version 0 → 1

		projekt.setName("Änderung A");
		assertThatThrownBy(() -> projectRepository.saveAndFlush(projekt))
			.isInstanceOf(OptimisticLockingFailureException.class);
	}

	@Test
	void benutzernameIstEindeutig() {
		em.flush();
		AppUser doppelt = new AppUser(organisation, "ma", "Noch jemand", "{noop}x", Role.MITARBEITER);

		assertThatThrownBy(() -> userRepository.saveAndFlush(doppelt))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void projektendeVorProjektbeginnVerletztCheckConstraint() {
		Project projekt = new Project(organisation, leitung, "Zeitreise");
		projekt.setStartDate(LocalDate.of(2026, 12, 10));
		projekt.setEndDate(LocalDate.of(2026, 12, 1));

		assertThatThrownBy(() -> projectRepository.saveAndFlush(projekt))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unbekannteRolleWirdVonDerDatenbankAbgelehnt() {
		em.flush();
		// Über JPA lässt sich keine ungültige Rolle speichern (Enum). Der CHECK-Constraint schützt zusätzlich
		// gegen direktes SQL, z. B. aus Migrationen oder Wartungsskripten.
		assertThatThrownBy(() -> jdbc.update(
				"insert into app_user (organisation_id, username, display_name, password_hash, role) values (?, 'gast', 'Gast', 'x', 'GAST')",
				organisation.getId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void projektMitAufgabenLaesstSichNichtLoeschen() {
		Project projekt = em.persist(new Project(organisation, leitung, "Bleibt"));
		em.persist(new Task(projekt, leitung, "Hängt dran"));
		em.flush();

		// Fremdschlüssel ohne ON DELETE CASCADE: Die Datenbank verweigert das Löschen (D5, Projekte nie löschen).
		assertThatThrownBy(() -> jdbc.update("delete from project where id = ?", projekt.getId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}

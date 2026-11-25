package io.github.tarahman.aufgabenmanager.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.KonfliktException;
import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.OrganisationRepository;
import io.github.tarahman.aufgabenmanager.common.UngueltigeAnfrageException;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektDaten;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/** Unit-Tests der Projektregeln ohne Spring und Datenbank (Mockito). */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

	@Mock
	ProjectRepository projectRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	OrganisationRepository organisationRepository;

	@InjectMocks
	ProjectService projectService;

	final Organisation firma = new Organisation("Testfirma");

	final CurrentUser leitung = new CurrentUser(2L, 10L, "leitung", "Lukas Leitung", Role.PROJEKTLEITER, true, null);

	final CurrentUser mitglied = new CurrentUser(4L, 10L, "mitarbeiter", "Mia", Role.MITARBEITER, true, null);

	Project projekt;

	@BeforeEach
	void projektVonLeitung() {
		AppUser lead = mitId(new AppUser(firma, "leitung", "Lukas Leitung", "x", Role.PROJEKTLEITER), 2L);
		projekt = new Project(firma, lead, "Projekt");
		ReflectionTestUtils.setField(projekt, "id", 1L);
		ReflectionTestUtils.setField(projekt, "version", 3L);
	}

	private void sichtbar() {
		given(projectRepository.findeSichtbar(eq(1L), anyLong(), anyLong(), anyBoolean())).willReturn(Optional.of(projekt));
	}

	@Test
	void leitungDarfVerwaltenMitgliedNicht() {
		assertThat(projectService.darfVerwalten(projekt, leitung)).isTrue();
		assertThat(projectService.darfVerwalten(projekt, mitglied)).isFalse();
	}

	@Test
	void projektleitungKannKeinProjektFuerAndereAnlegen() {
		ProjektDaten daten = new ProjektDaten("Neu", null, null, null, 3L, null);

		assertThatThrownBy(() -> projectService.anlegen(daten, leitung)).isInstanceOf(AccessDeniedException.class);
		then(projectRepository).should(never()).save(any());
	}

	@Test
	void endeVorBeginnWirdAbgelehnt() {
		ProjektDaten daten = new ProjektDaten("Neu", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 1, 1), null, null);

		assertThatThrownBy(() -> projectService.anlegen(daten, leitung)).isInstanceOf(UngueltigeAnfrageException.class);
	}

	@Test
	void veralteteVersionWirdAbgelehnt() {
		sichtbar();

		assertThatThrownBy(() -> projectService.aendern(1L, new ProjektDaten("Neu", null, null, null, null, 2L), leitung))
			.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(projekt.getName()).isEqualTo("Projekt");
	}

	@Test
	void archiviertesProjektIstSchreibgeschuetzt() {
		projekt.setArchivedAt(Instant.now());
		sichtbar();

		assertThatThrownBy(() -> projectService.aendern(1L, new ProjektDaten("Neu", null, null, null, null, 3L), leitung))
			.isInstanceOf(KonfliktException.class);
	}

	@Test
	void mitgliedBekommt403BevorArchivGeprueftWird() {
		projekt.setArchivedAt(Instant.now());
		sichtbar();

		// Reihenfolge der Prüfungen: erst Recht (403), dann Zustand (409).
		assertThatThrownBy(() -> projectService.archivieren(1L, mitglied)).isInstanceOf(AccessDeniedException.class);
	}

	private static AppUser mitId(AppUser user, Long id) {
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}

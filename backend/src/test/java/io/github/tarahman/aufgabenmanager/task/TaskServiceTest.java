package io.github.tarahman.aufgabenmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.UngueltigeAnfrageException;
import io.github.tarahman.aufgabenmanager.project.Project;
import io.github.tarahman.aufgabenmanager.project.ProjectService;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.AufgabeDaten;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Unit-Tests der Aufgabenregeln (Mockito). Der {@link ProjectService} ist hier eine Attrappe: Ob das Projekt
 * sichtbar ist, legt der Test fest; geprüft wird nur, was der TaskService selbst entscheidet.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	TaskRepository taskRepository;

	@Mock
	ProjectService projectService;

	@Mock
	UserRepository userRepository;

	@InjectMocks
	TaskService taskService;

	final Organisation firma = new Organisation("Testfirma");

	final CurrentUser mia = new CurrentUser(4L, 10L, "mitarbeiter", "Mia", Role.MITARBEITER, true, null);

	AppUser leitung;

	AppUser ehemalig;

	AppUser fremd;

	Project projekt;

	Task aufgabe;

	@BeforeEach
	void daten() {
		leitung = mitId(new AppUser(firma, "leitung", "Lukas", "x", Role.PROJEKTLEITER), 2L);
		ehemalig = mitId(new AppUser(firma, "ehemalig", "Erik", "x", Role.MITARBEITER), 6L);
		ehemalig.setActive(false);
		fremd = mitId(new AppUser(firma, "mitarbeiter2", "Tom", "x", Role.MITARBEITER), 5L);
		projekt = new Project(firma, leitung, "Kundenportal");
		ReflectionTestUtils.setField(projekt, "id", 1L);
		projekt.getMembers().add(ehemalig);

		aufgabe = new Task(projekt, leitung, "Von der Leitung angelegt");
		ReflectionTestUtils.setField(aufgabe, "id", 7L);
		ReflectionTestUtils.setField(aufgabe, "version", 0L);
		aufgabe.setAssignee(ehemalig);
		given(taskRepository.findById(7L)).willReturn(Optional.of(aufgabe));
		given(projectService.ladeSichtbar(1L, mia)).willReturn(projekt);
	}

	@Test
	void mitgliedDarfFremdeAufgabeNichtLoeschen() {
		given(projectService.darfVerwalten(projekt, mia)).willReturn(false);

		assertThatThrownBy(() -> taskService.loeschen(7L, mia)).isInstanceOf(AccessDeniedException.class);
		then(taskRepository).should(never()).delete(any());
	}

	@Test
	void nichtmitgliedKannNichtZustaendigWerden() {
		AufgabeDaten daten = new AufgabeDaten("Titel", null, fremd.getId(), null, null, 0L);

		assertThatThrownBy(() -> taskService.aendern(7L, daten, mia)).isInstanceOf(UngueltigeAnfrageException.class);
		assertThat(aufgabe.getAssignee()).isSameAs(ehemalig);
	}

	@Test
	void deaktivierteZustaendigkeitBleibtWennUnveraendert() {
		given(taskRepository.saveAndFlush(aufgabe)).willReturn(aufgabe);
		AufgabeDaten daten = new AufgabeDaten("Neuer Titel", null, ehemalig.getId(), null, null, 0L);

		taskService.aendern(7L, daten, mia);

		assertThat(aufgabe.getAssignee()).isSameAs(ehemalig);
		assertThat(aufgabe.getTitle()).isEqualTo("Neuer Titel");
	}

	private static AppUser mitId(AppUser user, Long id) {
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}

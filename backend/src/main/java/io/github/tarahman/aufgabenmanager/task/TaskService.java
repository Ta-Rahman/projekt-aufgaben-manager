package io.github.tarahman.aufgabenmanager.task;

import java.util.List;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.NichtGefundenException;
import io.github.tarahman.aufgabenmanager.common.UngueltigeAnfrageException;
import io.github.tarahman.aufgabenmanager.project.Project;
import io.github.tarahman.aufgabenmanager.project.ProjectService;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.AufgabeAntwort;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.AufgabeDaten;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.Person;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.StatusAendern;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Aufgaben (US4) und Statuswechsel (US5).
 *
 * <p>Rechte laut Matrix: Wer das Projekt sieht (ADMIN, Leitung, Mitglied), darf Aufgaben anlegen, bearbeiten
 * und den Status ändern. Löschen nur Leitung, ADMIN oder Ersteller:in (D5). Die Sichtbarkeit prüft der
 * {@link ProjectService}; diese Klasse kennt die Sichtbarkeitsregel nicht selbst.
 */
@Service
@Transactional
public class TaskService {

	private final TaskRepository taskRepository;

	private final ProjectService projectService;

	private final UserRepository userRepository;

	TaskService(TaskRepository taskRepository, ProjectService projectService, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.projectService = projectService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<AufgabeAntwort> board(Long projektId, CurrentUser angemeldet) {
		Project projekt = projectService.ladeSichtbar(projektId, angemeldet);
		return taskRepository.boardVonProjekt(projektId).stream().map(t -> zuAntwort(t, projekt, angemeldet)).toList();
	}

	public AufgabeAntwort anlegen(Long projektId, AufgabeDaten daten, CurrentUser angemeldet) {
		Project projekt = projectService.ladeSichtbar(projektId, angemeldet);
		projectService.pruefeNichtArchiviert(projekt);
		// getReferenceById: Ersteller:in nur als Fremdschlüssel nötig, kein SELECT.
		Task aufgabe = new Task(projekt, userRepository.getReferenceById(angemeldet.getId()), daten.titel().strip());
		uebernehme(aufgabe, daten, projekt);
		if (daten.status() != null) {
			aufgabe.setStatus(daten.status());
		}
		return zuAntwort(taskRepository.save(aufgabe), projekt, angemeldet);
	}

	public AufgabeAntwort aendern(Long id, AufgabeDaten daten, CurrentUser angemeldet) {
		Task aufgabe = ladeAenderbar(id, angemeldet);
		pruefeVersion(aufgabe, daten.version());
		aufgabe.setTitle(daten.titel().strip());
		uebernehme(aufgabe, daten, aufgabe.getProject());
		if (daten.status() != null) {
			aufgabe.setStatus(daten.status());
		}
		return zuAntwort(taskRepository.saveAndFlush(aufgabe), aufgabe.getProject(), angemeldet);
	}

	public AufgabeAntwort statusAendern(Long id, StatusAendern anfrage, CurrentUser angemeldet) {
		Task aufgabe = ladeAenderbar(id, angemeldet);
		pruefeVersion(aufgabe, anfrage.version());
		aufgabe.setStatus(anfrage.status());
		return zuAntwort(taskRepository.saveAndFlush(aufgabe), aufgabe.getProject(), angemeldet);
	}

	public void loeschen(Long id, CurrentUser angemeldet) {
		Task aufgabe = ladeAenderbar(id, angemeldet);
		if (!darfLoeschen(aufgabe, aufgabe.getProject(), angemeldet)) {
			throw new AccessDeniedException("Nur Leitung, Administration oder Ersteller:in");
		}
		taskRepository.delete(aufgabe);
	}

	/**
	 * Aufgabe laden, wenn ihr Projekt sichtbar ist (sonst 404) und nicht archiviert (sonst 409).
	 * {@code aufgabe.getProject().getId()} liest nur die ID aus dem Proxy, ohne SELECT.
	 */
	private Task ladeAenderbar(Long id, CurrentUser angemeldet) {
		Task aufgabe = taskRepository.findById(id)
			.orElseThrow(() -> new NichtGefundenException("Aufgabe nicht gefunden"));
		Project projekt;
		try {
			projekt = projectService.ladeSichtbar(aufgabe.getProject().getId(), angemeldet);
		}
		catch (NichtGefundenException e) {
			// Gleiche Meldung wie bei einer unbekannten ID: verrät nicht, dass es die Aufgabe gibt.
			throw new NichtGefundenException("Aufgabe nicht gefunden");
		}
		projectService.pruefeNichtArchiviert(projekt);
		return aufgabe;
	}

	/** Wie bei Projekten: Der Client schickt die Version mit, die er angezeigt hat (D8). */
	private static void pruefeVersion(Task aufgabe, Long versionDesClients) {
		if (versionDesClients == null) {
			throw new UngueltigeAnfrageException("Version fehlt");
		}
		if (!versionDesClients.equals(aufgabe.getVersion())) {
			throw new ObjectOptimisticLockingFailureException(Task.class, aufgabe.getId());
		}
	}

	private void uebernehme(Task aufgabe, AufgabeDaten daten, Project projekt) {
		aufgabe.setDescription(daten.beschreibung() == null || daten.beschreibung().isBlank() ? null : daten.beschreibung());
		aufgabe.setDueDate(daten.faelligAm());
		Long bisher = aufgabe.getAssignee() == null ? null : aufgabe.getAssignee().getId();
		if (Objects.equals(bisher, daten.zustaendigId())) {
			// Unverändert: Eine inzwischen deaktivierte Person darf zuständig bleiben (D6).
			return;
		}
		aufgabe.setAssignee(daten.zustaendigId() == null ? null : ladeMoeglicheZustaendige(daten.zustaendigId(), projekt));
	}

	/** Zuständig werden können nur aktive Mitglieder und die Leitung des Projekts (Regel aus den Wireframes). */
	private static AppUser ladeMoeglicheZustaendige(Long benutzerId, Project projekt) {
		AppUser kandidat = projekt.getLead().getId().equals(benutzerId) ? projekt.getLead()
				: projekt.getMembers().stream().filter(m -> m.getId().equals(benutzerId)).findFirst()
					.orElseThrow(() -> new UngueltigeAnfrageException("Zuständig kann nur ein Mitglied oder die Leitung sein"));
		if (!kandidat.isActive()) {
			throw new UngueltigeAnfrageException("Deaktivierte Benutzer können nicht zuständig werden");
		}
		return kandidat;
	}

	private boolean darfLoeschen(Task aufgabe, Project projekt, CurrentUser angemeldet) {
		return projectService.darfVerwalten(projekt, angemeldet)
				|| aufgabe.getCreatedBy().getId().equals(angemeldet.getId());
	}

	private AufgabeAntwort zuAntwort(Task t, Project projekt, CurrentUser angemeldet) {
		return new AufgabeAntwort(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getDueDate(),
				person(t.getAssignee()), person(t.getCreatedBy()), t.getVersion(), darfLoeschen(t, projekt, angemeldet));
	}

	private static Person person(AppUser u) {
		return u == null ? null : new Person(u.getId(), u.getDisplayName(), u.isActive());
	}

}

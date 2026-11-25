package io.github.tarahman.aufgabenmanager.project;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.KonfliktException;
import io.github.tarahman.aufgabenmanager.common.NichtGefundenException;
import io.github.tarahman.aufgabenmanager.common.UngueltigeAnfrageException;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Mitglieder eines Projekts (US3, Reiter Mitglieder P2). Sehen darf sie, wer das Projekt sieht;
 * ändern dürfen sie Leitung und ADMIN, und nur solange das Projekt nicht archiviert ist.
 */
@Service
@Transactional
public class MitgliederService {

	private final ProjectService projectService;

	private final UserRepository userRepository;

	MitgliederService(ProjectService projectService, UserRepository userRepository) {
		this.projectService = projectService;
		this.userRepository = userRepository;
	}

	public record Mitglied(Long id, String anzeigename, String benutzername, String rolle, boolean aktiv) {

		static Mitglied von(AppUser u) {
			return new Mitglied(u.getId(), u.getDisplayName(), u.getUsername(), u.getRole().name(), u.isActive());
		}

	}

	/** Leitung getrennt von den Mitgliedern, weil sie kein Mitglied ist (D2), aber oben mit angezeigt wird. */
	public record MitgliederAntwort(Mitglied leitung, List<Mitglied> mitglieder) {
	}

	@Transactional(readOnly = true)
	public MitgliederAntwort liste(Long projektId, CurrentUser angemeldet) {
		return antwort(projectService.ladeSichtbar(projektId, angemeldet));
	}

	public MitgliederAntwort hinzufuegen(Long projektId, Long benutzerId, CurrentUser angemeldet) {
		Project projekt = ladeAenderbar(projektId, angemeldet);
		AppUser neu = userRepository.findByIdAndOrganisationId(benutzerId, angemeldet.getOrganisationId())
			.orElseThrow(() -> new UngueltigeAnfrageException("Benutzer nicht gefunden"));
		if (!neu.isActive()) {
			throw new UngueltigeAnfrageException("Deaktivierte Benutzer können nicht hinzugefügt werden");
		}
		if (neu.getId().equals(projekt.getLead().getId())) {
			throw new UngueltigeAnfrageException("Die Projektleitung ist kein Mitglied");
		}
		// Set.add liefert false, wenn das Element schon enthalten ist. Gleichheit ist hier Objektidentität:
		// Innerhalb der Transaktion gibt es für jede Zeile genau ein AppUser-Objekt (Persistenzkontext).
		if (!projekt.getMembers().add(neu)) {
			throw new KonfliktException("Benutzer ist bereits Mitglied");
		}
		// Kein save(): Beim Commit schreibt Hibernate die neue Zeile in project_member.
		return antwort(projekt);
	}

	public MitgliederAntwort entfernen(Long projektId, Long benutzerId, CurrentUser angemeldet) {
		Project projekt = ladeAenderbar(projektId, angemeldet);
		// Zugewiesene Aufgaben bleiben zugewiesen; im Board ist die Person weiter als zuständig sichtbar.
		if (!projekt.getMembers().removeIf(m -> m.getId().equals(benutzerId))) {
			throw new NichtGefundenException("Mitglied nicht gefunden");
		}
		return antwort(projekt);
	}

	private Project ladeAenderbar(Long projektId, CurrentUser angemeldet) {
		Project projekt = projectService.ladeSichtbar(projektId, angemeldet);
		projectService.pruefeVerwaltbar(projekt, angemeldet);
		projectService.pruefeNichtArchiviert(projekt);
		return projekt;
	}

	private static MitgliederAntwort antwort(Project projekt) {
		List<Mitglied> mitglieder = projekt.getMembers()
			.stream()
			.sorted(Comparator.comparing(AppUser::getDisplayName))
			.map(Mitglied::von)
			.toList();
		return new MitgliederAntwort(Mitglied.von(projekt.getLead()), mitglieder);
	}

}

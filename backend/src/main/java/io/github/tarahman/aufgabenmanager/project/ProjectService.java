package io.github.tarahman.aufgabenmanager.project;

import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.KonfliktException;
import io.github.tarahman.aufgabenmanager.common.NichtGefundenException;
import io.github.tarahman.aufgabenmanager.common.OrganisationRepository;
import io.github.tarahman.aufgabenmanager.common.UngueltigeAnfrageException;
import io.github.tarahman.aufgabenmanager.common.Zeitstempel;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.Person;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektDaten;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektDetail;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektZeile;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Projekte (US2) mit Sichtbarkeit (US8) und Fortschritt (US6).
 *
 * <p>Drei Stufen der Prüfung, in dieser Reihenfolge:
 * <ol>
 *   <li>Sichtbar? Sonst 404, auch wenn das Projekt existiert (Existenz fremder Projekte wird nicht verraten).</li>
 *   <li>Darf verwalten (ADMIN oder Leitung)? Sonst 403: Das Projekt ist sichtbar, die Aktion aber nicht erlaubt.</li>
 *   <li>Nicht archiviert? Sonst 409: Archivierte Projekte sind schreibgeschützt (P6).</li>
 * </ol>
 * Andere Features (Mitglieder, Aufgaben) nutzen {@link #ladeSichtbar} und {@link #pruefeVerwaltbar}, damit
 * die Regeln nur hier stehen.
 */
@Service
@Transactional
public class ProjectService {

	private final ProjectRepository projectRepository;

	private final UserRepository userRepository;

	private final OrganisationRepository organisationRepository;

	ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
			OrganisationRepository organisationRepository) {
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.organisationRepository = organisationRepository;
	}

	@Transactional(readOnly = true)
	public List<ProjektZeile> uebersicht(boolean archiviert, CurrentUser angemeldet) {
		return projectRepository.uebersicht(angemeldet.getOrganisationId(), angemeldet.getId(),
				angemeldet.hasRole(Role.ADMIN), archiviert);
	}

	@Transactional(readOnly = true)
	public ProjektDetail detail(Long id, CurrentUser angemeldet) {
		return zuDetail(ladeSichtbar(id, angemeldet), angemeldet);
	}

	public ProjektDetail anlegen(ProjektDaten daten, CurrentUser angemeldet) {
		pruefeZeitraum(daten);
		Long leitungId = daten.leitungId() != null ? daten.leitungId() : angemeldet.getId();
		// Projektleitung kann nur für sich selbst anlegen; sonst hätte sie danach keinen Zugriff auf das Projekt.
		if (angemeldet.hasRole(Role.PROJEKTLEITER) && !leitungId.equals(angemeldet.getId())) {
			throw new AccessDeniedException("Projektleitung kann nur eigene Projekte anlegen");
		}
		Project projekt = new Project(organisationRepository.getReferenceById(angemeldet.getOrganisationId()),
				ladeMoeglicheLeitung(leitungId, angemeldet), daten.name().strip());
		uebernehme(projekt, daten);
		return zuDetail(projectRepository.save(projekt), angemeldet);
	}

	public ProjektDetail aendern(Long id, ProjektDaten daten, CurrentUser angemeldet) {
		Project projekt = ladeSichtbar(id, angemeldet);
		pruefeVerwaltbar(projekt, angemeldet);
		pruefeNichtArchiviert(projekt);
		pruefeVersion(projekt, daten.version());
		pruefeZeitraum(daten);
		if (daten.leitungId() != null && !daten.leitungId().equals(projekt.getLead().getId())) {
			projekt.setLead(ladeMoeglicheLeitung(daten.leitungId(), angemeldet));
		}
		projekt.setName(daten.name().strip());
		uebernehme(projekt, daten);
		// saveAndFlush statt Dirty Checking am Transaktionsende: So steht die erhöhte Version schon in der Antwort.
		return zuDetail(projectRepository.saveAndFlush(projekt), angemeldet);
	}

	public ProjektDetail archivieren(Long id, CurrentUser angemeldet) {
		Project projekt = ladeSichtbar(id, angemeldet);
		pruefeVerwaltbar(projekt, angemeldet);
		pruefeNichtArchiviert(projekt);
		projekt.setArchivedAt(Zeitstempel.jetzt());
		return zuDetail(projectRepository.saveAndFlush(projekt), angemeldet);
	}

	public ProjektDetail wiederherstellen(Long id, CurrentUser angemeldet) {
		Project projekt = ladeSichtbar(id, angemeldet);
		pruefeVerwaltbar(projekt, angemeldet);
		if (!projekt.isArchived()) {
			throw new KonfliktException("Projekt ist nicht archiviert");
		}
		projekt.setArchivedAt(null);
		return zuDetail(projectRepository.saveAndFlush(projekt), angemeldet);
	}

	/** Stufe 1: sichtbar oder 404 (US8). */
	public Project ladeSichtbar(Long id, CurrentUser angemeldet) {
		return projectRepository
			.findeSichtbar(id, angemeldet.getOrganisationId(), angemeldet.getId(), angemeldet.hasRole(Role.ADMIN))
			.orElseThrow(() -> new NichtGefundenException("Projekt nicht gefunden"));
	}

	public boolean darfVerwalten(Project projekt, CurrentUser angemeldet) {
		return angemeldet.hasRole(Role.ADMIN) || projekt.getLead().getId().equals(angemeldet.getId());
	}

	/** Stufe 2: ADMIN oder Leitung, sonst 403. */
	public void pruefeVerwaltbar(Project projekt, CurrentUser angemeldet) {
		if (!darfVerwalten(projekt, angemeldet)) {
			throw new AccessDeniedException("Nur Projektleitung oder Administration");
		}
	}

	/** Stufe 3: archivierte Projekte sind schreibgeschützt (P6). */
	public void pruefeNichtArchiviert(Project projekt) {
		if (projekt.isArchived()) {
			throw new KonfliktException("Projekt ist archiviert und kann nicht geändert werden");
		}
	}

	/**
	 * Optimistisches Sperren über HTTP hinweg (D8). {@code @Version} allein schützt nur innerhalb einer
	 * Transaktion: Hier wird das Projekt frisch geladen, hätte also immer die aktuelle Version. Deshalb
	 * schickt der Client die Version mit, die er angezeigt hat, und wir vergleichen selbst.
	 */
	private void pruefeVersion(Project projekt, Long versionDesClients) {
		if (versionDesClients == null) {
			throw new UngueltigeAnfrageException("Version fehlt");
		}
		if (!versionDesClients.equals(projekt.getVersion())) {
			throw new ObjectOptimisticLockingFailureException(Project.class, projekt.getId());
		}
	}

	private void pruefeZeitraum(ProjektDaten daten) {
		if (daten.startDatum() != null && daten.endDatum() != null && daten.endDatum().isBefore(daten.startDatum())) {
			throw new UngueltigeAnfrageException("Ende liegt vor dem Beginn");
		}
	}

	/** Leitung muss aktiv sein, zur Organisation gehören und die Rolle PROJEKTLEITER oder ADMIN haben (D6). */
	private AppUser ladeMoeglicheLeitung(Long leitungId, CurrentUser angemeldet) {
		AppUser leitung = userRepository.findByIdAndOrganisationId(leitungId, angemeldet.getOrganisationId())
			.orElseThrow(() -> new UngueltigeAnfrageException("Leitung nicht gefunden"));
		if (!leitung.isActive() || leitung.getRole() == Role.MITARBEITER) {
			throw new UngueltigeAnfrageException("Leitung muss aktiv sein und Projektleitung oder Administration haben");
		}
		return leitung;
	}

	private static void uebernehme(Project projekt, ProjektDaten daten) {
		projekt.setDescription(daten.beschreibung() == null || daten.beschreibung().isBlank() ? null : daten.beschreibung());
		projekt.setStartDate(daten.startDatum());
		projekt.setEndDate(daten.endDatum());
	}

	private ProjektDetail zuDetail(Project p, CurrentUser angemeldet) {
		// p.getLead() ist ein LAZY-Proxy; getDisplayName() lädt die Leitung nach. Das geht, weil wir noch
		// in der Transaktion des Service sind (open-in-view=false: im Controller ginge es nicht mehr).
		AppUser leitung = p.getLead();
		return new ProjektDetail(p.getId(), p.getName(), p.getDescription(), p.getStartDate(), p.getEndDate(),
				new Person(leitung.getId(), leitung.getDisplayName()), p.getArchivedAt(), p.getVersion(),
				projectRepository.fortschritt(p.getId()), darfVerwalten(p, angemeldet));
	}

}

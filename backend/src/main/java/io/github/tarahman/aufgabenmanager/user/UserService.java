package io.github.tarahman.aufgabenmanager.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.KonfliktException;
import io.github.tarahman.aufgabenmanager.common.NichtGefundenException;
import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.OrganisationRepository;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAendern;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAnlegen;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAntwort;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAuswahl;

/**
 * Benutzerverwaltung (US1). Wer das darf (nur ADMIN), prüft der Controller per {@code @PreAuthorize};
 * der Service sorgt dafür, dass nur Benutzer der eigenen Organisation sichtbar und änderbar sind (E2b).
 *
 * <p>{@code @Transactional} an der Klasse: Jede öffentliche Methode läuft in einer Transaktion. Spring legt dazu
 * einen Proxy um den Service, der vor dem Aufruf die Transaktion öffnet und danach committet (bei
 * RuntimeException: Rollback).
 */
@Service
@Transactional
public class UserService {

	private final UserRepository userRepository;

	private final OrganisationRepository organisationRepository;

	private final PasswordEncoder passwordEncoder;

	UserService(UserRepository userRepository, OrganisationRepository organisationRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.organisationRepository = organisationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<BenutzerAntwort> alle(CurrentUser angemeldet) {
		return userRepository.findByOrganisationIdOrderByDisplayNameAsc(angemeldet.getOrganisationId())
			.stream()
			.map(BenutzerAntwort::von)
			.toList();
	}

	/** Nur aktive Benutzer: Deaktivierte sind nicht auswählbar (D6). Ohne Benutzername und Status. */
	@Transactional(readOnly = true)
	public List<BenutzerAuswahl> auswahl(CurrentUser angemeldet) {
		return userRepository.findByOrganisationIdAndActiveTrueOrderByDisplayNameAsc(angemeldet.getOrganisationId())
			.stream()
			.map(u -> new BenutzerAuswahl(u.getId(), u.getDisplayName(), u.getRole()))
			.toList();
	}

	public BenutzerAntwort anlegen(BenutzerAnlegen anfrage, CurrentUser angemeldet) {
		if (userRepository.existsByUsername(anfrage.benutzername())) {
			throw new KonfliktException("Benutzername ist bereits vergeben");
		}
		// getReferenceById lädt die Organisation nicht, sondern liefert einen Proxy mit der ID.
		// Für den Fremdschlüssel organisation_id reicht das, ein SELECT wäre überflüssig.
		Organisation organisation = organisationRepository.getReferenceById(angemeldet.getOrganisationId());
		AppUser neu = new AppUser(organisation, anfrage.benutzername(), anfrage.anzeigename().strip(),
				passwordEncoder.encode(anfrage.passwort()), anfrage.rolle());
		return BenutzerAntwort.von(userRepository.save(neu));
	}

	public BenutzerAntwort aendern(Long id, BenutzerAendern anfrage, CurrentUser angemeldet) {
		AppUser user = ladeInEigenerOrganisation(id, angemeldet);

		// Schutz vor Selbstaussperrung: Der letzte Weg zurück in die Benutzerverwaltung wäre sonst die Datenbank.
		if (user.getId().equals(angemeldet.getId()) && (!anfrage.aktiv() || anfrage.rolle() != Role.ADMIN)) {
			throw new KonfliktException("Das eigene Konto kann nicht deaktiviert oder herabgestuft werden");
		}

		user.setDisplayName(anfrage.anzeigename().strip());
		user.setRole(anfrage.rolle());
		user.setActive(anfrage.aktiv());
		if (anfrage.passwort() != null) {
			user.setPasswordHash(passwordEncoder.encode(anfrage.passwort()));
		}
		// Kein save() nötig: user ist im Persistenzkontext, Hibernate erkennt die Änderungen beim Commit
		// (Dirty Checking) und schreibt ein UPDATE.
		return BenutzerAntwort.von(user);
	}

	private AppUser ladeInEigenerOrganisation(Long id, CurrentUser angemeldet) {
		return userRepository.findByIdAndOrganisationId(id, angemeldet.getOrganisationId())
			.orElseThrow(() -> new NichtGefundenException("Benutzer nicht gefunden"));
	}

}

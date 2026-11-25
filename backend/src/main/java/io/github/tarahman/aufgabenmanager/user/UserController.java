package io.github.tarahman.aufgabenmanager.user;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAendern;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAnlegen;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAntwort;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAuswahl;
import jakarta.validation.Valid;

/**
 * REST-Schnittstelle der Benutzerverwaltung (US1, Seite P5 {@code /benutzer}).
 * Kein DELETE: Benutzer werden nur deaktiviert (D5, D6).
 *
 * <p>{@code @PreAuthorize} an der Klasse gilt für alle Methoden: Spring Security prüft die Rolle, bevor die
 * Methode läuft, und antwortet sonst mit 403.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	List<BenutzerAntwort> alle(@AuthenticationPrincipal CurrentUser angemeldet) {
		return userService.alle(angemeldet);
	}

	/**
	 * Auswahlliste aktiver Benutzer der eigenen Organisation (Leitung im Projektdialog, später Mitglieder und
	 * Zuständige). Für alle Angemeldeten: Die Methoden-Annotation ersetzt die der Klasse.
	 */
	@GetMapping("/auswahl")
	@PreAuthorize("isAuthenticated()")
	List<BenutzerAuswahl> auswahl(@AuthenticationPrincipal CurrentUser angemeldet) {
		return userService.auswahl(angemeldet);
	}

	/** 201 Created mit Location-Header auf den neuen Benutzer. */
	@PostMapping
	ResponseEntity<BenutzerAntwort> anlegen(@Valid @RequestBody BenutzerAnlegen anfrage,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		BenutzerAntwort neu = userService.anlegen(anfrage, angemeldet);
		return ResponseEntity.created(URI.create("/api/users/" + neu.id())).body(neu);
	}

	@PutMapping("/{id}")
	BenutzerAntwort aendern(@PathVariable Long id, @Valid @RequestBody BenutzerAendern anfrage,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return userService.aendern(id, anfrage, angemeldet);
	}

}

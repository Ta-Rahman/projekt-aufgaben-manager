package io.github.tarahman.aufgabenmanager.project;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.project.MitgliederService.MitgliederAntwort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Mitglieder eines Projekts (US3). Hinzufügen und Entfernen antworten mit der aktualisierten Liste,
 * damit das Frontend den Reiter ohne zweite Anfrage neu zeichnen kann.
 */
@RestController
@RequestMapping("/api/projects/{projektId}/members")
class MitgliederController {

	private final MitgliederService mitgliederService;

	MitgliederController(MitgliederService mitgliederService) {
		this.mitgliederService = mitgliederService;
	}

	record MitgliedHinzufuegen(@NotNull(message = "Benutzer fehlt") Long benutzerId) {
	}

	@GetMapping
	MitgliederAntwort liste(@PathVariable Long projektId, @AuthenticationPrincipal CurrentUser angemeldet) {
		return mitgliederService.liste(projektId, angemeldet);
	}

	@PostMapping
	MitgliederAntwort hinzufuegen(@PathVariable Long projektId, @Valid @RequestBody MitgliedHinzufuegen anfrage,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return mitgliederService.hinzufuegen(projektId, anfrage.benutzerId(), angemeldet);
	}

	@DeleteMapping("/{benutzerId}")
	MitgliederAntwort entfernen(@PathVariable Long projektId, @PathVariable Long benutzerId,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return mitgliederService.entfernen(projektId, benutzerId, angemeldet);
	}

}

package io.github.tarahman.aufgabenmanager.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tarahman.aufgabenmanager.user.Role;

/**
 * Hilfsendpunkte rund um die Sitzung. Login und Logout selbst übernimmt Spring Security
 * (siehe {@link SecurityConfig}).
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

	/**
	 * Liefert das CSRF-Token und setzt dabei das Cookie {@code XSRF-TOKEN}.
	 * Die SPA ruft das einmal beim Start auf, damit der erste POST (Login) ein Token hat.
	 */
	@GetMapping("/csrf")
	CsrfAntwort csrf(CsrfToken token) {
		return new CsrfAntwort(token.getHeaderName(), token.getToken());
	}

	/**
	 * Gibt den angemeldeten Benutzer zurück; ohne Anmeldung antwortet die Security-Kette mit 401.
	 * Die SPA steuert damit Navigation und sichtbare Aktionen (z. B. Menüpunkt Benutzer nur für ADMIN).
	 * {@code @AuthenticationPrincipal} holt das beim Login abgelegte {@link CurrentUser} aus der Sitzung.
	 */
	@GetMapping("/me")
	BenutzerAntwort me(@AuthenticationPrincipal CurrentUser benutzer) {
		return new BenutzerAntwort(benutzer.getId(), benutzer.getUsername(), benutzer.getDisplayName(),
				benutzer.getRole());
	}

	record CsrfAntwort(String headerName, String token) {
	}

	record BenutzerAntwort(Long id, String benutzername, String anzeigename, Role rolle) {
	}

}

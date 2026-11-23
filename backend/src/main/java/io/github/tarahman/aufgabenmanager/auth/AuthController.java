package io.github.tarahman.aufgabenmanager.auth;

import java.security.Principal;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	/** Gibt den angemeldeten Benutzer zurück; ohne Anmeldung antwortet die Security-Kette mit 401. */
	@GetMapping("/me")
	BenutzerAntwort me(Principal principal) {
		return new BenutzerAntwort(principal.getName());
	}

	record CsrfAntwort(String headerName, String token) {
	}

	record BenutzerAntwort(String benutzername) {
	}

}

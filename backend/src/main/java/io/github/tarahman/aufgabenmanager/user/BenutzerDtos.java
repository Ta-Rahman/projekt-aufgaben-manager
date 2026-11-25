package io.github.tarahman.aufgabenmanager.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request- und Response-Objekte der Benutzerverwaltung (US1). Entitäten gehen nie direkt als JSON hinaus:
 * So steht hier ausdrücklich, welche Felder die API zeigt (kein Passwort-Hash) und welche sie annimmt.
 *
 * <p>Die Annotationen sind Bean Validation: {@code @Valid} im Controller prüft sie, bevor die Methode läuft,
 * und ein Verstoß wird zu 400 mit Feldliste (siehe {@code GlobalExceptionHandler}).
 */
public final class BenutzerDtos {

	private BenutzerDtos() {
	}

	public record BenutzerAntwort(Long id, String benutzername, String anzeigename, Role rolle, boolean aktiv) {

		static BenutzerAntwort von(AppUser user) {
			return new BenutzerAntwort(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
					user.isActive());
		}

	}

	/** Eintrag für Auswahllisten; für alle Angemeldeten sichtbar, daher nur das Nötigste. */
	public record BenutzerAuswahl(Long id, String anzeigename, Role rolle) {
	}

	public record BenutzerAnlegen(
			@NotBlank(message = "Benutzername fehlt")
			@Size(min = 3, max = 50, message = "Benutzername muss 3 bis 50 Zeichen lang sein")
			@Pattern(regexp = "[a-z0-9._-]*", message = "Benutzername: nur Kleinbuchstaben, Ziffern, Punkt, Minus, Unterstrich")
			String benutzername,

			@NotBlank(message = "Name fehlt")
			@Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein")
			String anzeigename,

			@NotNull(message = "Rolle fehlt")
			Role rolle,

			// BCrypt verarbeitet höchstens 72 Byte, längere Passwörter würden stillschweigend abgeschnitten.
			@NotBlank(message = "Passwort fehlt")
			@Size(min = 8, max = 72, message = "Passwort muss 8 bis 72 Zeichen lang sein")
			String passwort) {
	}

	/** Bearbeiten: Benutzername ist unveränderlich; {@code passwort = null} lässt das Passwort unverändert. */
	public record BenutzerAendern(
			@NotBlank(message = "Name fehlt")
			@Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein")
			String anzeigename,

			@NotNull(message = "Rolle fehlt")
			Role rolle,

			boolean aktiv,

			@Size(min = 8, max = 72, message = "Passwort muss 8 bis 72 Zeichen lang sein")
			String passwort) {
	}

}

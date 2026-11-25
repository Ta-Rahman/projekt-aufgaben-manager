package io.github.tarahman.aufgabenmanager.task;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request- und Response-Objekte für Aufgaben (US4, US5). */
public final class AufgabenDtos {

	private AufgabenDtos() {
	}

	public record Person(Long id, String anzeigename, boolean aktiv) {
	}

	/**
	 * Eine Karte im Board (P1) bzw. der Inhalt des Aufgabendialogs. {@code zustaendig} ist {@code null}, wenn
	 * niemand zugewiesen ist. {@code darfLoeschen}: Leitung, ADMIN oder Ersteller:in (D5).
	 */
	public record AufgabeAntwort(Long id, String titel, String beschreibung, TaskStatus status, LocalDate faelligAm,
			Person zustaendig, Person erstelltVon, long version, boolean darfLoeschen) {
	}

	/**
	 * Anlegen ({@code POST}) und Bearbeiten ({@code PUT}). {@code status = null}: beim Anlegen OFFEN, beim
	 * Bearbeiten unverändert. {@code version} ist beim Bearbeiten Pflicht (D8).
	 */
	public record AufgabeDaten(
			@NotBlank(message = "Titel fehlt")
			@Size(max = 200, message = "Titel darf höchstens 200 Zeichen lang sein")
			String titel,

			@Size(max = 5000, message = "Beschreibung darf höchstens 5000 Zeichen lang sein")
			String beschreibung,

			Long zustaendigId,

			LocalDate faelligAm,

			TaskStatus status,

			Long version) {
	}

	/** Statuswechsel direkt auf der Karte im Board (US5). */
	public record StatusAendern(
			@NotNull(message = "Status fehlt")
			TaskStatus status,

			@NotNull(message = "Version fehlt")
			Long version) {
	}

}

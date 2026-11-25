package io.github.tarahman.aufgabenmanager.project;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request- und Response-Objekte für Projekte (US2, US6, US8). */
public final class ProjektDtos {

	private ProjektDtos() {
	}

	/** Fortschritt (US6): wird nie gespeichert, sondern bei jeder Abfrage aus den Aufgaben gezählt. */
	public record Fortschritt(long erledigt, long gesamt) {

		/**
		 * Ganzzahlig gerundeter Anteil; ein Projekt ohne Aufgaben hat 0 %. Jackson serialisiert bei Records
		 * nur die Komponenten; {@code @JsonProperty} nimmt diese berechnete Methode mit ins JSON.
		 */
		@JsonProperty
		public int prozent() {
			return gesamt == 0 ? 0 : (int) Math.round(100.0 * erledigt / gesamt);
		}

	}

	/**
	 * Eine Zeile der Projektübersicht (P4). Wird direkt von der JPQL-Abfrage im {@link ProjectRepository}
	 * erzeugt ({@code select new ...}), also ohne Entitäten zu laden: eine SQL-Abfrage für die ganze Liste.
	 */
	public record ProjektZeile(Long id, String name, String leitung, LocalDate startDatum, LocalDate endDatum,
			boolean archiviert, Fortschritt fortschritt) {

		/** Konstruktor für die JPQL-Abfrage; {@code sum(...)} liefert bei leerer Menge {@code null}. */
		public ProjektZeile(Long id, String name, String leitung, LocalDate startDatum, LocalDate endDatum,
				Instant archiviertAm, Long erledigt, Long gesamt) {
			this(id, name, leitung, startDatum, endDatum, archiviertAm != null,
					new Fortschritt(erledigt == null ? 0 : erledigt, gesamt == null ? 0 : gesamt));
		}

	}

	public record Person(Long id, String anzeigename) {
	}

	/**
	 * Projektkopf für das Projektdetail (P1/P2). {@code version} braucht das Frontend, um beim Speichern
	 * zu zeigen, welchen Stand es bearbeitet hat (D8). {@code darfVerwalten} sagt dem Frontend, ob es
	 * Bearbeiten, Archivieren und Mitgliederverwaltung anzeigen soll; geprüft wird trotzdem im Backend.
	 */
	public record ProjektDetail(Long id, String name, String beschreibung, LocalDate startDatum, LocalDate endDatum,
			Person leitung, Instant archiviertAm, long version, Fortschritt fortschritt, boolean darfVerwalten) {
	}

	/**
	 * Anlegen ({@code POST}) und Bearbeiten ({@code PUT}).
	 * {@code leitungId = null} beim Anlegen: Die angemeldete Person wird Leitung.
	 * {@code version} ist nur beim Bearbeiten Pflicht: der Stand, den der Client geladen hat (optimistisches Sperren, D8).
	 */
	public record ProjektDaten(
			@NotBlank(message = "Name fehlt")
			@Size(max = 200, message = "Name darf höchstens 200 Zeichen lang sein")
			String name,

			@Size(max = 5000, message = "Beschreibung darf höchstens 5000 Zeichen lang sein")
			String beschreibung,

			LocalDate startDatum,

			LocalDate endDatum,

			Long leitungId,

			Long version) {
	}

}

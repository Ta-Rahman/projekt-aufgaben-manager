package io.github.tarahman.aufgabenmanager.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Einheitliche Quelle für Zeitstempel der Entitäten.
 *
 * <p>PostgreSQL speichert {@code timestamptz} mit Mikrosekunden. Java liefert je nach System feiner aufgelöste
 * Werte; ohne Abschneiden stünde im Objekt ein anderer Wert als nach dem Neuladen aus der Datenbank.
 */
public final class Zeitstempel {

	private Zeitstempel() {
	}

	public static Instant jetzt() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}

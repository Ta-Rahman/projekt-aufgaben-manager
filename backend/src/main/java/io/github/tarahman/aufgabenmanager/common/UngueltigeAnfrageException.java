package io.github.tarahman.aufgabenmanager.common;

/**
 * Fachlich ungültige Eingabe, die Bean Validation allein nicht erkennen kann (z. B. Ende vor Beginn,
 * Leitung ohne passende Rolle); wird zu HTTP 400.
 */
public class UngueltigeAnfrageException extends RuntimeException {

	public UngueltigeAnfrageException(String meldung) {
		super(meldung);
	}

}

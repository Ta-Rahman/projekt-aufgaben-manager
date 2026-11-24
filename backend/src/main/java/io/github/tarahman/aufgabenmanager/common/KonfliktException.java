package io.github.tarahman.aufgabenmanager.common;

/**
 * Fachlicher Fehler „Anfrage ist gültig, passt aber nicht zum aktuellen Zustand“ (z. B. Benutzername
 * schon vergeben); wird zu HTTP 409.
 */
public class KonfliktException extends RuntimeException {

	public KonfliktException(String meldung) {
		super(meldung);
	}

}

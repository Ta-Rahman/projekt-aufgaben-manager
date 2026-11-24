package io.github.tarahman.aufgabenmanager.common;

/**
 * Fachlicher Fehler „gibt es nicht oder nicht für dich“; wird zu HTTP 404.
 * Auch für Objekte anderer Organisationen oder ohne Sichtrecht, damit deren Existenz nicht verraten wird.
 */
public class NichtGefundenException extends RuntimeException {

	public NichtGefundenException(String meldung) {
		super(meldung);
	}

}

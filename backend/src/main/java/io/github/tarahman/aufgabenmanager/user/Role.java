package io.github.tarahman.aufgabenmanager.user;

/**
 * Globale Rolle eines Benutzers (eine je Benutzer, E3a). Als Text gespeichert (D4): Die Reihenfolge der
 * Konstanten ist egal, Umbenennen erfordert aber eine Migration (CHECK-Constraint und vorhandene Daten).
 */
public enum Role {
	ADMIN,
	PROJEKTLEITER,
	MITARBEITER
}

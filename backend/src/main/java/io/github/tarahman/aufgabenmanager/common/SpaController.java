package io.github.tarahman.aufgabenmanager.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Liefert für Adressen der Single-Page-App ({@code /projekte/2}, {@code /benutzer}, ...) die {@code index.html}
 * aus, damit Neuladen und direkte Links funktionieren. Das Routing übernimmt danach React Router im Browser.
 *
 * <p>Ohne diese Weiterleitung würde Spring beim Neuladen von {@code /projekte/2} eine Datei oder einen
 * Controller zu diesem Pfad suchen und 404 antworten. {@code forward:} ist eine interne Weiterleitung
 * (keine neue Adresse im Browser).
 *
 * <p>Die Muster erfassen ein- und zweistufige Pfade ohne Punkt (also keine Dateien wie {@code app.js}) und
 * nicht {@code /api}: API-Aufrufe ohne passenden Controller sollen weiter 404 liefern, nicht HTML.
 */
@Controller
class SpaController {

	@GetMapping({ "/{pfad:^(?!api$)[^.]*}", "/{pfad:^(?!api$)[^.]*}/{weiter:[^.]*}" })
	String index() {
		return "forward:/index.html";
	}

}

package io.github.tarahman.aufgabenmanager.project;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektDaten;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektDetail;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektZeile;
import jakarta.validation.Valid;

/**
 * REST-Schnittstelle für Projekte (US2, US6, US8; Seiten Projektübersicht und Projektdetail).
 * Kein DELETE: Projekte werden archiviert (D5). Die Objektrechte prüft der {@link ProjectService}.
 */
@RestController
@RequestMapping("/api/projects")
class ProjectController {

	private final ProjectService projectService;

	ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	/** {@code ?archiviert=true} für den Filter „Archiviert“ (P6); Standard: aktive Projekte. */
	@GetMapping
	List<ProjektZeile> uebersicht(@RequestParam(defaultValue = "false") boolean archiviert,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return projectService.uebersicht(archiviert, angemeldet);
	}

	@GetMapping("/{id}")
	ProjektDetail detail(@PathVariable Long id, @AuthenticationPrincipal CurrentUser angemeldet) {
		return projectService.detail(id, angemeldet);
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'PROJEKTLEITER')")
	ResponseEntity<ProjektDetail> anlegen(@Valid @RequestBody ProjektDaten daten,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		ProjektDetail neu = projectService.anlegen(daten, angemeldet);
		return ResponseEntity.created(URI.create("/api/projects/" + neu.id())).body(neu);
	}

	@PutMapping("/{id}")
	ProjektDetail aendern(@PathVariable Long id, @Valid @RequestBody ProjektDaten daten,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return projectService.aendern(id, daten, angemeldet);
	}

	@PostMapping("/{id}/archive")
	ProjektDetail archivieren(@PathVariable Long id, @AuthenticationPrincipal CurrentUser angemeldet) {
		return projectService.archivieren(id, angemeldet);
	}

	@PostMapping("/{id}/restore")
	ProjektDetail wiederherstellen(@PathVariable Long id, @AuthenticationPrincipal CurrentUser angemeldet) {
		return projectService.wiederherstellen(id, angemeldet);
	}

}

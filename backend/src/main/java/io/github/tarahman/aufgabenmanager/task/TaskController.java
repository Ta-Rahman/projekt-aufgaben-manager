package io.github.tarahman.aufgabenmanager.task;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.AufgabeAntwort;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.AufgabeDaten;
import io.github.tarahman.aufgabenmanager.task.AufgabenDtos.StatusAendern;
import jakarta.validation.Valid;

/**
 * REST-Schnittstelle für Aufgaben (US4, US5). Liste und Anlegen hängen am Projekt, Bearbeiten und Löschen
 * adressieren die Aufgabe direkt (Pfade laut Arbeitsplan 4.2).
 */
@RestController
class TaskController {

	private final TaskService taskService;

	TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping("/api/projects/{projektId}/tasks")
	List<AufgabeAntwort> board(@PathVariable Long projektId, @AuthenticationPrincipal CurrentUser angemeldet) {
		return taskService.board(projektId, angemeldet);
	}

	@PostMapping("/api/projects/{projektId}/tasks")
	ResponseEntity<AufgabeAntwort> anlegen(@PathVariable Long projektId, @Valid @RequestBody AufgabeDaten daten,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		AufgabeAntwort neu = taskService.anlegen(projektId, daten, angemeldet);
		return ResponseEntity.created(URI.create("/api/tasks/" + neu.id())).body(neu);
	}

	@PutMapping("/api/tasks/{id}")
	AufgabeAntwort aendern(@PathVariable Long id, @Valid @RequestBody AufgabeDaten daten,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return taskService.aendern(id, daten, angemeldet);
	}

	/** PATCH, weil nur ein Teil der Aufgabe geändert wird (der Status), PUT ersetzt dagegen alle Felder. */
	@PatchMapping("/api/tasks/{id}/status")
	AufgabeAntwort statusAendern(@PathVariable Long id, @Valid @RequestBody StatusAendern anfrage,
			@AuthenticationPrincipal CurrentUser angemeldet) {
		return taskService.statusAendern(id, anfrage, angemeldet);
	}

	@DeleteMapping("/api/tasks/{id}")
	ResponseEntity<Void> loeschen(@PathVariable Long id, @AuthenticationPrincipal CurrentUser angemeldet) {
		taskService.loeschen(id, angemeldet);
		return ResponseEntity.noContent().build();
	}

}

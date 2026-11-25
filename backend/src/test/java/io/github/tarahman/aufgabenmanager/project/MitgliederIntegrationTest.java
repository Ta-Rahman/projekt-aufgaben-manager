package io.github.tarahman.aufgabenmanager.project;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import io.github.tarahman.aufgabenmanager.ApiIntegrationTest;
import jakarta.persistence.EntityManager;

/** US3 über die REST-Schnittstelle gegen PostgreSQL mit Seed-Daten (siehe {@link ApiIntegrationTest}). */
class MitgliederIntegrationTest extends ApiIntegrationTest {

	@Autowired
	EntityManager entityManager;

	private ResultActions hinzufuegen(String projekt, String wer, String neu) throws Exception {
		return mockMvc.perform(post("/api/projects/{id}/members", projektId(projekt)).with(als(wer)).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"benutzerId\": %d}".formatted(benutzerId(neu))));
	}

	@Test
	void mitgliedSiehtLeitungUndMitglieder() throws Exception {
		mockMvc.perform(get("/api/projects/{id}/members", projektId(KUNDENPORTAL)).with(als("mitarbeiter")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.leitung.anzeigename").value("Lukas Leitung"))
			.andExpect(jsonPath("$.mitglieder[*].anzeigename", contains("Erik Ehemalig", "Mia Mitarbeiter")))
			.andExpect(jsonPath("$.mitglieder[0].aktiv").value(false));
	}

	@Test
	void leitungFuegtMitgliedHinzuOhneDieProjektversionZuAendern() throws Exception {
		hinzufuegen(KUNDENPORTAL, "leitung", "mitarbeiter2")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mitglieder[*].benutzername", hasItem("mitarbeiter2")));

		// Erst flush schreibt die Änderung in die Datenbank (und würde dabei die Version erhöhen),
		// clear erzwingt, dass das Projekt danach frisch aus PostgreSQL gelesen wird.
		entityManager.flush();
		entityManager.clear();
		// @OptimisticLock(excluded = true) an Project.members: Wer parallel den Projektkopf bearbeitet,
		// bekommt deshalb keinen 409.
		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("leitung")))
			.andExpect(jsonPath("$.version").value(0));
		// Neues Mitglied sieht das Projekt jetzt (US8).
		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("mitarbeiter2")))
			.andExpect(status().isOk());
	}

	@Test
	void doppeltHinzufuegenLiefert409() throws Exception {
		hinzufuegen(KUNDENPORTAL, "leitung", "mitarbeiter").andExpect(status().isConflict());
	}

	@Test
	void leitungUndDeaktivierteSindKeineMoeglichenMitglieder() throws Exception {
		hinzufuegen(KUNDENPORTAL, "admin", "leitung")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Die Projektleitung ist kein Mitglied"));

		userRepository.findByUsername("mitarbeiter2").orElseThrow().setActive(false);
		hinzufuegen(KUNDENPORTAL, "leitung", "mitarbeiter2").andExpect(status().isBadRequest());
	}

	@Test
	void mitgliedDarfKeineMitgliederVerwalten() throws Exception {
		hinzufuegen(KUNDENPORTAL, "mitarbeiter", "mitarbeiter2").andExpect(status().isForbidden());
	}

	@Test
	void unsichtbaresProjektLiefert404() throws Exception {
		mockMvc.perform(get("/api/projects/{id}/members", projektId(KUNDENPORTAL)).with(als("leitung2")))
			.andExpect(status().isNotFound());
	}

	@Test
	void entfernenBehaeltZustaendigkeiten() throws Exception {
		mockMvc.perform(delete("/api/projects/{id}/members/{u}", projektId(KUNDENPORTAL), benutzerId("ehemalig"))
				.with(als("leitung")).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mitglieder[*].benutzername", contains("mitarbeiter")));

		mockMvc.perform(get("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("leitung")))
			.andExpect(jsonPath("$[*].zustaendig.anzeigename", hasItem("Erik Ehemalig")));
	}

	@Test
	void nichtMitgliedEntfernenLiefert404() throws Exception {
		mockMvc.perform(delete("/api/projects/{id}/members/{u}", projektId(KUNDENPORTAL), benutzerId("mitarbeiter2"))
				.with(als("leitung")).with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void archiviertesProjektIstGesperrt() throws Exception {
		hinzufuegen(INVENTUR, "leitung", "mitarbeiter2").andExpect(status().isConflict());
	}

}

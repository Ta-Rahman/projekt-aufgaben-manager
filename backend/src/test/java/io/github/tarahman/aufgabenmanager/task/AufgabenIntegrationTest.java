package io.github.tarahman.aufgabenmanager.task;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import io.github.tarahman.aufgabenmanager.ApiIntegrationTest;

/**
 * US4 und US5 über die REST-Schnittstelle gegen PostgreSQL mit Seed-Daten (siehe {@link ApiIntegrationTest}).
 * Im Kundenportal hat {@code leitung} vier Aufgaben angelegt, {@code mitarbeiter} eine
 * („Altdaten der Kundenkonten prüfen“, zuständig: der deaktivierte {@code ehemalig}).
 */
class AufgabenIntegrationTest extends ApiIntegrationTest {

	static final String ALTDATEN = "Altdaten der Kundenkonten prüfen";

	static final String SSO = "Login an Kunden-SSO anbinden";

	@Autowired
	TaskRepository taskRepository;

	private Long aufgabeId(String titel) {
		return taskRepository.findAll().stream().filter(t -> t.getTitle().equals(titel)).findFirst().orElseThrow().getId();
	}

	private ResultActions statusSetzen(String titel, String wer, String neuerStatus, long version) throws Exception {
		return mockMvc.perform(patch("/api/tasks/{id}/status", aufgabeId(titel)).with(als(wer)).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"%s\", \"version\": %d}".formatted(neuerStatus, version)));
	}

	@Test
	void boardZeigtAlleAufgabenDesProjekts() throws Exception {
		mockMvc.perform(get("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("mitarbeiter")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[?(@.titel == 'Lasttest vorbereiten')].zustaendig").value(org.hamcrest.Matchers.contains((Object) null)))
			.andExpect(jsonPath("$[?(@.titel == '%s')].zustaendig.aktiv".formatted(ALTDATEN)).value(org.hamcrest.Matchers.contains(false)))
			// Ersteller:in darf löschen, sonst nur Leitung und ADMIN (D5).
			.andExpect(jsonPath("$[?(@.titel == '%s')].darfLoeschen".formatted(ALTDATEN)).value(org.hamcrest.Matchers.contains(true)))
			.andExpect(jsonPath("$[?(@.titel == '%s')].darfLoeschen".formatted(SSO)).value(org.hamcrest.Matchers.contains(false)));
	}

	@Test
	void unsichtbaresProjektUndFremdeAufgabeLiefern404() throws Exception {
		mockMvc.perform(get("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("mitarbeiter2")))
			.andExpect(status().isNotFound());
		statusSetzen(SSO, "mitarbeiter2", "ERLEDIGT", 0).andExpect(status().isNotFound());
	}

	@Test
	void mitgliedLegtAufgabeAn() throws Exception {
		mockMvc.perform(post("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("mitarbeiter")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"titel": "Barrierefreiheit prüfen", "zustaendigId": %d, "faelligAm": "2026-12-18"}
						""".formatted(benutzerId("leitung"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("OFFEN"))
			.andExpect(jsonPath("$.zustaendig.anzeigename").value("Lukas Leitung"))
			.andExpect(jsonPath("$.erstelltVon.anzeigename").value("Mia Mitarbeiter"))
			.andExpect(jsonPath("$.darfLoeschen").value(true));

		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("mitarbeiter")))
			.andExpect(jsonPath("$.fortschritt.gesamt").value(6));
	}

	@Test
	void zustaendigNurAktiveMitgliederOderLeitung() throws Exception {
		String vorlage = """
				{"titel": "Test", "zustaendigId": %d}
				""";
		// mitarbeiter2 ist nicht Mitglied im Kundenportal
		mockMvc.perform(post("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(vorlage.formatted(benutzerId("mitarbeiter2"))))
			.andExpect(status().isBadRequest());
		// ehemalig ist Mitglied, aber deaktiviert (D6)
		mockMvc.perform(post("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(vorlage.formatted(benutzerId("ehemalig"))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void bestehendeZustaendigkeitEinesDeaktiviertenBleibtBeimBearbeiten() throws Exception {
		mockMvc.perform(put("/api/tasks/{id}", aufgabeId(ALTDATEN)).with(als("mitarbeiter")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"titel": "Altdaten prüfen (dringend)", "zustaendigId": %d, "version": 0}
						""".formatted(benutzerId("ehemalig"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.titel").value("Altdaten prüfen (dringend)"))
			.andExpect(jsonPath("$.zustaendig.anzeigename").value("Erik Ehemalig"))
			.andExpect(jsonPath("$.status").value("OFFEN"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void statuswechselAendertDenFortschritt() throws Exception {
		statusSetzen(SSO, "mitarbeiter", "ERLEDIGT", 0)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ERLEDIGT"));

		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("leitung")))
			.andExpect(jsonPath("$.fortschritt.erledigt").value(3))
			.andExpect(jsonPath("$.fortschritt.prozent").value(60));
	}

	@Test
	void statuswechselMitVeralteterVersionLiefert409() throws Exception {
		statusSetzen(SSO, "mitarbeiter", "ERLEDIGT", 0).andExpect(status().isOk());
		statusSetzen(SSO, "leitung", "OFFEN", 0).andExpect(status().isConflict());
	}

	@Test
	void ungueltigerStatusOderTitelLiefert400() throws Exception {
		statusSetzen(SSO, "mitarbeiter", "VIELLEICHT", 0).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"titel\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fehler[0].feld").value("titel"));
	}

	@Test
	void loeschenNurLeitungAdminOderErsteller() throws Exception {
		mockMvc.perform(delete("/api/tasks/{id}", aufgabeId(SSO)).with(als("mitarbeiter")).with(csrf()))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/tasks/{id}", aufgabeId(ALTDATEN)).with(als("mitarbeiter")).with(csrf()))
			.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/tasks/{id}", aufgabeId(SSO)).with(als("leitung")).with(csrf()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/projects/{id}/tasks", projektId(KUNDENPORTAL)).with(als("leitung")))
			.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	void archiviertesProjektIstSchreibgeschuetzt() throws Exception {
		statusSetzen("Geräteliste exportieren", "leitung", "OFFEN", 0).andExpect(status().isConflict());
		mockMvc.perform(post("/api/projects/{id}/tasks", projektId(INVENTUR)).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"titel\": \"Nachzügler\"}"))
			.andExpect(status().isConflict());
		// Lesen bleibt erlaubt.
		mockMvc.perform(get("/api/projects/{id}/tasks", projektId(INVENTUR)).with(als("mitarbeiter")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));
	}

}

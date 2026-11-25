package io.github.tarahman.aufgabenmanager.project;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.TestcontainersConfiguration;
import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * US2, US6 und US8 über die REST-Schnittstelle gegen PostgreSQL mit den Seed-Daten (V2):
 *
 * <pre>
 * Relaunch Kundenportal   Leitung leitung,  Mitglieder mitarbeiter, ehemalig;   2 von 5 erledigt
 * Migration Mailserver    Leitung leitung2, Mitglieder mitarbeiter, mitarbeiter2; 1 von 3 erledigt
 * Inventur Hardware 2025  Leitung leitung,  Mitglied mitarbeiter, archiviert;   2 von 2 erledigt
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProjekteIntegrationTest {

	static final String KUNDENPORTAL = "Relaunch Kundenportal";

	static final String MAILSERVER = "Migration Mailserver";

	static final String INVENTUR = "Inventur Hardware 2025";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRepository projectRepository;

	private RequestPostProcessor als(String benutzername) {
		AppUser u = userRepository.findByUsername(benutzername).orElseThrow();
		return user(new CurrentUser(u.getId(), u.getOrganisation().getId(), u.getUsername(), u.getDisplayName(),
				u.getRole(), u.isActive(), null));
	}

	private Long projektId(String name) {
		return projectRepository.findAll().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow().getId();
	}

	private Long benutzerId(String benutzername) {
		return userRepository.findByUsername(benutzername).orElseThrow().getId();
	}

	// ---------- US8: Sichtbarkeit ----------

	/** Erwartete Projekte je Benutzer, durch | getrennt, nach Name sortiert; leer = keine. */
	@ParameterizedTest(name = "{0}: aktiv [{1}], archiviert [{2}]")
	@CsvSource(delimiter = ';', value = {
			"admin;        Migration Mailserver|Relaunch Kundenportal; Inventur Hardware 2025",
			"leitung;      Relaunch Kundenportal;                       Inventur Hardware 2025",
			"leitung2;     Migration Mailserver;                        ''",
			"mitarbeiter;  Migration Mailserver|Relaunch Kundenportal; Inventur Hardware 2025",
			"mitarbeiter2; Migration Mailserver;                        ''" })
	void jederSiehtNurSeineProjekte(String benutzer, String aktiv, String archiviert) throws Exception {
		pruefeListe(benutzer, false, aktiv);
		pruefeListe(benutzer, true, archiviert);
	}

	private void pruefeListe(String benutzer, boolean archiviert, String erwartet) throws Exception {
		var anfrage = mockMvc.perform(get("/api/projects").param("archiviert", String.valueOf(archiviert)).with(als(benutzer)))
			.andExpect(status().isOk());
		if (erwartet.isEmpty()) {
			anfrage.andExpect(jsonPath("$", empty()));
		}
		else {
			anfrage.andExpect(jsonPath("$[*].name", contains(Arrays.stream(erwartet.split("\\|")).map(String::strip).toArray())));
		}
	}

	@Test
	void fremdesProjektLiefert404StattDessenExistenzZuVerraten() throws Exception {
		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("mitarbeiter2")))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/projects/{id}", 999_999).with(als("mitarbeiter2")))
			.andExpect(status().isNotFound());
	}

	@Test
	void mitgliedSiehtDetailDarfAberNichtVerwalten() throws Exception {
		mockMvc.perform(get("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("mitarbeiter")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.leitung.anzeigename").value("Lukas Leitung"))
			.andExpect(jsonPath("$.darfVerwalten").value(false));
	}

	// ---------- US6: Fortschritt ----------

	@Test
	void fortschrittWirdAusDenAufgabenBerechnet() throws Exception {
		mockMvc.perform(get("/api/projects").with(als("admin")))
			.andExpect(jsonPath("$[0].name").value(MAILSERVER))
			.andExpect(jsonPath("$[0].fortschritt.erledigt").value(1))
			.andExpect(jsonPath("$[0].fortschritt.gesamt").value(3))
			.andExpect(jsonPath("$[0].fortschritt.prozent").value(33))
			.andExpect(jsonPath("$[1].fortschritt.prozent").value(40));
		mockMvc.perform(get("/api/projects/{id}", projektId(INVENTUR)).with(als("leitung")))
			.andExpect(jsonPath("$.fortschritt.prozent").value(100));
	}

	@Test
	void projektOhneAufgabenHatNullProzent() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Leeres Projekt"}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.fortschritt.gesamt").value(0))
			.andExpect(jsonPath("$.fortschritt.prozent").value(0));
	}

	// ---------- US2: Anlegen ----------

	@Test
	void projektleitungLegtProjektFuerSichAn() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("leitung2")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Schulung Datenschutz", "startDatum": "2027-01-05", "endDatum": "2027-01-30"}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.leitung.anzeigename").value("Petra Planer"))
			.andExpect(jsonPath("$.version").value(0))
			.andExpect(jsonPath("$.darfVerwalten").value(true));
	}

	@Test
	void mitarbeiterDarfKeinProjektAnlegen() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("mitarbeiter")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Heimlich"}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void projektleitungDarfKeinProjektFuerAndereAnlegen() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Für Petra", "leitungId": %d}
						""".formatted(benutzerId("leitung2"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminSetztLeitungAberNurMitPassenderRolle() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Für Petra", "leitungId": %d}
						""".formatted(benutzerId("leitung2"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.leitung.anzeigename").value("Petra Planer"));
		mockMvc.perform(post("/api/projects").with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Für Mia", "leitungId": %d}
						""".formatted(benutzerId("mitarbeiter"))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void ungueltigeDatenLiefern400() throws Exception {
		mockMvc.perform(post("/api/projects").with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": " "}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fehler[0].feld").value("name"));
		mockMvc.perform(post("/api/projects").with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Zeitreise", "startDatum": "2027-02-01", "endDatum": "2027-01-01"}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ende liegt vor dem Beginn"));
	}

	// ---------- US2: Bearbeiten mit optimistischem Sperren (D8) ----------

	@Test
	void leitungBearbeitetUndVersionSteigt() throws Exception {
		mockMvc.perform(put("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Relaunch Kundenportal 2.0", "version": 0}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Relaunch Kundenportal 2.0"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void veralteteVersionLiefert409() throws Exception {
		Long id = projektId(KUNDENPORTAL);
		String aenderung = """
				{"name": "Erster gewinnt", "version": 0}
				""";
		mockMvc.perform(put("/api/projects/{id}", id).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(aenderung))
			.andExpect(status().isOk());
		// Zweite Person hat noch Version 0 angezeigt bekommen.
		mockMvc.perform(put("/api/projects/{id}", id).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(aenderung))
			.andExpect(status().isConflict());
	}

	@Test
	void mitgliedDarfNichtBearbeiten() throws Exception {
		mockMvc.perform(put("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("mitarbeiter")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Übernommen", "version": 0}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void andereProjektleitungBekommt404() throws Exception {
		// leitung2 ist weder Leitung noch Mitglied im Kundenportal: unsichtbar, also 404 statt 403.
		mockMvc.perform(put("/api/projects/{id}", projektId(KUNDENPORTAL)).with(als("leitung2")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Übernommen", "version": 0}
						"""))
			.andExpect(status().isNotFound());
	}

	// ---------- US2: Archivieren (P6) ----------

	@Test
	void archivierenWiederherstellenUndSchreibschutz() throws Exception {
		Long id = projektId(KUNDENPORTAL);

		mockMvc.perform(post("/api/projects/{id}/archive", id).with(als("leitung")).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.archiviertAm").isNotEmpty());
		mockMvc.perform(post("/api/projects/{id}/archive", id).with(als("leitung")).with(csrf()))
			.andExpect(status().isConflict());
		mockMvc.perform(put("/api/projects/{id}", id).with(als("leitung")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Trotzdem", "version": 1}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Projekt ist archiviert und kann nicht geändert werden"));

		mockMvc.perform(post("/api/projects/{id}/restore", id).with(als("admin")).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.archiviertAm").isEmpty());
	}

	@Test
	void mitgliedDarfNichtArchivieren() throws Exception {
		mockMvc.perform(post("/api/projects/{id}/archive", projektId(KUNDENPORTAL)).with(als("mitarbeiter")).with(csrf()))
			.andExpect(status().isForbidden());
	}

	@Test
	void auswahlListeEnthaeltNurAktiveBenutzer() throws Exception {
		mockMvc.perform(get("/api/users/auswahl").with(als("leitung")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].anzeigename", contains("Anna Admin", "Lukas Leitung", "Mia Mitarbeiter",
					"Petra Planer", "Tom Tester")));
	}

}

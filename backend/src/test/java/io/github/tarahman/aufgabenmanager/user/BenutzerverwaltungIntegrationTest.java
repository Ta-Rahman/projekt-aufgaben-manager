package io.github.tarahman.aufgabenmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.TestcontainersConfiguration;
import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.OrganisationRepository;

/**
 * US1 über die REST-Schnittstelle: echte Security-Kette, echte Validierung, echte Datenbank mit Seed (V2).
 *
 * <p>{@code @Transactional} an der Testklasse: Jeder Test läuft in einer Transaktion, die am Ende
 * zurückgerollt wird. MockMvc ruft den Controller im selben Thread auf, daher gehören auch die
 * Datenbankzugriffe der Anwendung zu dieser Transaktion. Die Tests beeinflussen sich so nicht gegenseitig.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BenutzerverwaltungIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	OrganisationRepository organisationRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	/** Meldet einen Seed-Benutzer für eine einzelne Anfrage an, ohne den Login-Ablauf zu durchlaufen. */
	private RequestPostProcessor als(String benutzername) {
		AppUser u = userRepository.findByUsername(benutzername).orElseThrow();
		return user(new CurrentUser(u.getId(), u.getOrganisation().getId(), u.getUsername(), u.getDisplayName(),
				u.getRole(), u.isActive(), null));
	}

	@Test
	void adminSiehtAlleBenutzerDerOrganisation() throws Exception {
		mockMvc.perform(get("/api/users").with(als("admin")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(6)))
			.andExpect(jsonPath("$[0].anzeigename").value("Anna Admin"))
			// Die Antwort enthält keinen Passwort-Hash.
			.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}

	@ParameterizedTest(name = "{0} bekommt 403")
	@ValueSource(strings = { "leitung", "mitarbeiter" })
	void nurAdminDarfBenutzerVerwalten(String benutzername) throws Exception {
		mockMvc.perform(get("/api/users").with(als(benutzername)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/users").with(als(benutzername)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(neuerBenutzer("neu.person", "MITARBEITER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void ohneAnmeldung401() throws Exception {
		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
	}

	@Test
	void adminLegtBenutzerAnUndPasswortWirdGehasht() throws Exception {
		mockMvc.perform(post("/api/users").with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(neuerBenutzer("neu.person", "PROJEKTLEITER")))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/users/")))
			.andExpect(jsonPath("$.benutzername").value("neu.person"))
			.andExpect(jsonPath("$.rolle").value("PROJEKTLEITER"))
			.andExpect(jsonPath("$.aktiv").value(true));

		AppUser gespeichert = userRepository.findByUsername("neu.person").orElseThrow();
		assertThat(gespeichert.getPasswordHash()).startsWith("{bcrypt}");
		assertThat(passwordEncoder.matches("geheim123", gespeichert.getPasswordHash())).isTrue();
	}

	@Test
	void vergebenerBenutzernameLiefert409() throws Exception {
		mockMvc.perform(post("/api/users").with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(neuerBenutzer("leitung", "MITARBEITER")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Benutzername ist bereits vergeben"));
	}

	@Test
	void ungueltigeEingabenLiefern400MitFeldliste() throws Exception {
		mockMvc.perform(post("/api/users").with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"benutzername": "Mit Leerzeichen", "anzeigename": " ", "passwort": "kurz"}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.fehler[*].feld",
					containsInAnyOrder("benutzername", "anzeigename", "rolle", "passwort")));
	}

	@Test
	void adminAendertRolleUndDeaktiviert() throws Exception {
		Long id = userRepository.findByUsername("mitarbeiter2").orElseThrow().getId();

		mockMvc.perform(put("/api/users/{id}", id).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"anzeigename": "Tom Tester", "rolle": "PROJEKTLEITER", "aktiv": false}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rolle").value("PROJEKTLEITER"))
			.andExpect(jsonPath("$.aktiv").value(false));

		AppUser geaendert = userRepository.findById(id).orElseThrow();
		assertThat(geaendert.isActive()).isFalse();
		// Ohne Feld "passwort" bleibt das alte Passwort gültig.
		assertThat(passwordEncoder.matches("demo", geaendert.getPasswordHash())).isTrue();
	}

	@Test
	void adminSetztNeuesPasswort() throws Exception {
		Long id = userRepository.findByUsername("mitarbeiter").orElseThrow().getId();

		mockMvc.perform(put("/api/users/{id}", id).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"anzeigename": "Mia Mitarbeiter", "rolle": "MITARBEITER", "aktiv": true, "passwort": "neuesPasswort"}
						"""))
			.andExpect(status().isOk());

		assertThat(passwordEncoder.matches("neuesPasswort",
				userRepository.findById(id).orElseThrow().getPasswordHash())).isTrue();
	}

	@Test
	void adminKannSichNichtSelbstAussperren() throws Exception {
		Long id = userRepository.findByUsername("admin").orElseThrow().getId();

		mockMvc.perform(put("/api/users/{id}", id).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"anzeigename": "Anna Admin", "rolle": "ADMIN", "aktiv": false}
						"""))
			.andExpect(status().isConflict());
	}

	@Test
	void unbekannteIdLiefert404() throws Exception {
		mockMvc.perform(put("/api/users/{id}", 999_999).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"anzeigename": "X", "rolle": "MITARBEITER", "aktiv": true}
						"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Benutzer nicht gefunden"));
	}

	@Test
	void benutzerAndererOrganisationenSindUnsichtbar() throws Exception {
		Organisation fremd = organisationRepository.save(new Organisation("Andere Firma"));
		AppUser fremderBenutzer = userRepository.save(
				new AppUser(fremd, "fremd", "Fremde Person", "{noop}x", Role.MITARBEITER));

		mockMvc.perform(get("/api/users").with(als("admin")))
			.andExpect(jsonPath("$[*].benutzername", hasSize(6)));
		mockMvc.perform(put("/api/users/{id}", fremderBenutzer.getId()).with(als("admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"anzeigename": "Übernommen", "rolle": "ADMIN", "aktiv": true}
						"""))
			.andExpect(status().isNotFound());
	}

	private static String neuerBenutzer(String benutzername, String rolle) {
		return """
				{"benutzername": "%s", "anzeigename": "Neue Person", "rolle": "%s", "passwort": "geheim123"}
				""".formatted(benutzername, rolle);
	}

}

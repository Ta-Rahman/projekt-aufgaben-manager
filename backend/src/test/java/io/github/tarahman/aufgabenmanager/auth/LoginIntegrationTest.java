package io.github.tarahman.aufgabenmanager.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import io.github.tarahman.aufgabenmanager.TestcontainersConfiguration;

/**
 * US7 Ende zu Ende im Backend: echte Security-Kette, echter {@link AppUserDetailsService},
 * Benutzer aus den Seed-Daten (V2) in PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@ParameterizedTest(name = "{0} meldet sich an und hat Rolle {1}")
	@CsvSource({
			"admin,        ADMIN",
			"leitung,      PROJEKTLEITER",
			"mitarbeiter,  MITARBEITER" })
	void seedBenutzerKoennenSichAnmelden(String benutzername, String rolle) throws Exception {
		MockHttpSession sitzung = anmelden(benutzername, "demo");

		// Zweite Anfrage mit derselben Sitzung: Der Server kennt den Benutzer jetzt ohne erneutes Passwort.
		mockMvc.perform(get("/api/auth/me").session(sitzung))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.benutzername").value(benutzername))
			.andExpect(jsonPath("$.rolle").value(rolle));
	}

	@Test
	void falschesPasswortLiefert401() throws Exception {
		mockMvc.perform(post("/api/auth/login").with(csrf()).param("username", "admin").param("password", "falsch"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deaktivierterBenutzerKannSichNichtAnmelden() throws Exception {
		// "ehemalig" ist im Seed mit active = false angelegt (D6); Passwort wäre richtig.
		mockMvc.perform(post("/api/auth/login").with(csrf()).param("username", "ehemalig").param("password", "demo"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void nachLogoutIstDieSitzungUngueltig() throws Exception {
		MockHttpSession sitzung = anmelden("leitung", "demo");

		mockMvc.perform(post("/api/auth/logout").with(csrf()).session(sitzung))
			.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/auth/me").session(sitzung))
			.andExpect(status().isUnauthorized());
	}

	private MockHttpSession anmelden(String benutzername, String passwort) throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
				.param("username", benutzername).param("password", passwort))
			.andExpect(status().isNoContent())
			.andReturn().getRequest().getSession();
	}

}

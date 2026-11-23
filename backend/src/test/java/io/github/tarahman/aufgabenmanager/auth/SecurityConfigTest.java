package io.github.tarahman.aufgabenmanager.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-Slice-Test der Sicherheitskonfiguration: nur Web-Schicht und Security, keine Datenbank.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void apiOhneAnmeldungLiefert401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void apiMitAnmeldungLiefertBenutzer() throws Exception {
		mockMvc.perform(get("/api/auth/me").with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.benutzername").value("demo"));
	}

	@Test
	void csrfEndpunktIstFreiUndSetztCookie() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andExpect(cookie().httpOnly("XSRF-TOKEN", false))
			.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
	}

	@Test
	void loginOhneCsrfTokenWirdAbgelehnt() throws Exception {
		mockMvc.perform(post("/api/auth/login").param("username", "demo").param("password", "demo"))
			.andExpect(status().isForbidden());
	}

	@Test
	void loginMitRichtigenZugangsdatenLiefert204() throws Exception {
		mockMvc.perform(post("/api/auth/login").with(csrf()).param("username", "demo").param("password", "demo"))
			.andExpect(status().isNoContent());
	}

	@Test
	void loginMitFalschemPasswortLiefert401() throws Exception {
		mockMvc.perform(post("/api/auth/login").with(csrf()).param("username", "demo").param("password", "falsch"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutLiefert204() throws Exception {
		mockMvc.perform(post("/api/auth/logout").with(csrf()).with(user("demo")))
			.andExpect(status().isNoContent());
	}

	@Test
	void seitenAusserhalbDerApiSindFrei() throws Exception {
		// Kein Controller und keine statische Datei im Slice-Test: 404, aber eben kein 401/403.
		mockMvc.perform(get("/projekte"))
			.andExpect(status().isNotFound());
	}

}

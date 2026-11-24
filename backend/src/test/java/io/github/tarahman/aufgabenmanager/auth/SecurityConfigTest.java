package io.github.tarahman.aufgabenmanager.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Web-Slice-Test der Sicherheitskonfiguration: nur Web-Schicht und Security, keine Datenbank.
 * Das Repository ist durch ein Mockito-Double ersetzt ({@code @MockitoBean}); jeder Test legt fest,
 * welchen Benutzer es liefert. Gegen die echte Datenbank prüft {@code LoginIntegrationTest}.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, AppUserDetailsService.class })
class SecurityConfigTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoBean
	UserRepository userRepository;

	@BeforeEach
	void benutzerImRepository() {
		AppUser demo = new AppUser(new Organisation("Testfirma"), "demo", "Demo Benutzer",
				passwordEncoder.encode("demo"), Role.MITARBEITER);
		given(userRepository.findByUsername("demo")).willReturn(Optional.of(demo));
	}

	@Test
	void apiOhneAnmeldungLiefert401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void apiMitAnmeldungLiefertBenutzer() throws Exception {
		CurrentUser angemeldet = new CurrentUser(7L, 1L, "demo", "Demo Benutzer", Role.ADMIN, true, null);
		mockMvc.perform(get("/api/auth/me").with(user(angemeldet)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(7))
			.andExpect(jsonPath("$.benutzername").value("demo"))
			.andExpect(jsonPath("$.anzeigename").value("Demo Benutzer"))
			.andExpect(jsonPath("$.rolle").value("ADMIN"));
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
	void unbekannterBenutzerLiefert401() throws Exception {
		mockMvc.perform(post("/api/auth/login").with(csrf()).param("username", "niemand").param("password", "demo"))
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

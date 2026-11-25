package io.github.tarahman.aufgabenmanager.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.tarahman.aufgabenmanager.auth.SecurityConfig;

/** Web-Slice-Test der SPA-Weiterleitung, ohne Datenbank. */
@WebMvcTest(SpaController.class)
@Import(SecurityConfig.class)
class SpaControllerTest {

	@Autowired
	MockMvc mockMvc;

	@ParameterizedTest(name = "{0} → index.html")
	@ValueSource(strings = { "/login", "/projekte", "/projekte/2", "/benutzer", "/gibt-es-nicht" })
	void spaAdressenLiefernIndexHtml(String pfad) throws Exception {
		mockMvc.perform(get(pfad)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
	}

	@ParameterizedTest(name = "{0} wird nicht weitergeleitet")
	@ValueSource(strings = { "/assets/index.js", "/favicon.ico", "/api/gibt-es-nicht", "/api" })
	void dateienWerdenNichtWeitergeleitet(String pfad) throws Exception {
		mockMvc.perform(get(pfad)).andExpect(forwardedUrl(null));
	}

}

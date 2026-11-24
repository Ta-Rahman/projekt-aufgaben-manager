package io.github.tarahman.aufgabenmanager.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Sicherheitskonfiguration für eine Single-Page-App auf derselben Origin.
 *
 * <ul>
 *   <li>Sitzungsbasiert: Nach dem Login hält der Server die Anmeldung in der HTTP-Sitzung,
 *       der Browser trägt nur das HttpOnly-Sitzungscookie {@code JSESSIONID}.</li>
 *   <li>CSRF-Schutz über Cookie {@code XSRF-TOKEN} (für JavaScript lesbar); die SPA schickt den Wert
 *       bei ändernden Anfragen im Header {@code X-XSRF-TOKEN} zurück.</li>
 *   <li>{@code /api/**} erfordert Anmeldung und antwortet mit Statuscodes statt Weiterleitungen,
 *       alles andere (gebautes Frontend, statische Dateien) ist frei.</li>
 *   <li>Benutzer kommen aus der Datenbank ({@link AppUserDetailsService}); Spring Security verbindet
 *       den einzigen {@code UserDetailsService} und den {@code PasswordEncoder} automatisch mit dem Login.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
// Schaltet @PreAuthorize an Controller- und Service-Methoden ein (Rollenprüfung je Methode).
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/csrf").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.csrf(csrf -> csrf.spa())
			// Formular-Login als API: POST /api/auth/login mit username/password (form-urlencoded),
			// Antwort 204 bei Erfolg, 401 bei falschen Zugangsdaten, keine Weiterleitung.
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/api/auth/login")
				.successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
				.failureHandler(new SimpleUrlAuthenticationFailureHandler())
				.permitAll())
			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			// Nicht angemeldet: 401 statt Weiterleitung auf eine Login-Seite.
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.httpBasic(basic -> basic.disable());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// Standard-Encoder mit Präfix ({bcrypt}...), damit der Algorithmus später gewechselt werden kann.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}

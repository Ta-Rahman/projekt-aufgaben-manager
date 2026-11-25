package io.github.tarahman.aufgabenmanager;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.project.ProjectRepository;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Gemeinsame Basis der API-Integrationstests: kompletter Anwendungskontext, MockMvc, PostgreSQL per
 * Testcontainers mit den Seed-Daten aus V2.
 *
 * <p>{@code @Transactional}: Jeder Test läuft in einer Transaktion, die am Ende zurückgerollt wird. MockMvc ruft
 * den Controller im selben Thread auf, daher gehören auch die Datenbankzugriffe der Anwendung dazu. Die Tests
 * beeinflussen sich so nicht gegenseitig. Alle Unterklassen teilen sich denselben Spring-Kontext und Container.
 *
 * <p>Seed-Daten:
 * <pre>
 * Relaunch Kundenportal   Leitung leitung,  Mitglieder mitarbeiter, ehemalig (deaktiviert); 2 von 5 erledigt
 * Migration Mailserver    Leitung leitung2, Mitglieder mitarbeiter, mitarbeiter2;           1 von 3 erledigt
 * Inventur Hardware 2025  Leitung leitung,  Mitglied mitarbeiter, archiviert;              2 von 2 erledigt
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class ApiIntegrationTest {

	protected static final String KUNDENPORTAL = "Relaunch Kundenportal";

	protected static final String MAILSERVER = "Migration Mailserver";

	protected static final String INVENTUR = "Inventur Hardware 2025";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected ProjectRepository projectRepository;

	/** Meldet einen Seed-Benutzer für eine einzelne Anfrage an, ohne den Login-Ablauf zu durchlaufen. */
	protected RequestPostProcessor als(String benutzername) {
		AppUser u = userRepository.findByUsername(benutzername).orElseThrow();
		return user(new CurrentUser(u.getId(), u.getOrganisation().getId(), u.getUsername(), u.getDisplayName(),
				u.getRole(), u.isActive(), null));
	}

	protected Long projektId(String name) {
		return projectRepository.findAll()
			.stream()
			.filter(p -> p.getName().equals(name))
			.findFirst()
			.orElseThrow()
			.getId();
	}

	protected Long benutzerId(String benutzername) {
		return userRepository.findByUsername(benutzername).orElseThrow().getId();
	}

}

package io.github.tarahman.aufgabenmanager;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integrationstest: startet den vollständigen Spring-Kontext gegen einen echten
 * PostgreSQL-Container (Testcontainers) und prüft, dass Datenbank und Flyway angebunden sind.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AufgabenManagerApplicationTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	Flyway flyway;

	@Test
	void kontextStartetGegenPostgresql() {
		String version = jdbcTemplate.queryForObject("SHOW server_version", String.class);
		assertThat(version).startsWith("18.");
	}

	@Test
	void flywayIstAktiv() {
		Integer historientabellen = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
				Integer.class);
		assertThat(historientabellen).isEqualTo(1);
		assertThat(flyway.info()).isNotNull();
	}

	@Test
	void seedDatenSindEingespielt() {
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM organisation", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(6);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM project WHERE archived_at IS NULL", Integer.class))
			.isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM task", Integer.class)).isEqualTo(10);
	}

}

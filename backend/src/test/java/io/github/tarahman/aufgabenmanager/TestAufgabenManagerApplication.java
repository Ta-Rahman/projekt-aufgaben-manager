package io.github.tarahman.aufgabenmanager;

import org.springframework.boot.SpringApplication;

/**
 * Startet die Anwendung mit einer frischen PostgreSQL-Datenbank aus Testcontainers statt der Compose-Datenbank:
 * {@code mvnw spring-boot:test-run}. Bei jedem Start legen Flyway V1 und V2 Schema und Seed-Daten neu an.
 * Grundlage für den Playwright-E2E-Test und die Screenshots, damit beide immer denselben Datenstand sehen.
 */
public class TestAufgabenManagerApplication {

	public static void main(String[] args) {
		// Sonst würde Spring Boot zusätzlich die Datenbank aus docker-compose.yml verwenden.
		System.setProperty("spring.docker.compose.enabled", "false");
		SpringApplication.from(AufgabenManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

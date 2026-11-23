package io.github.tarahman.aufgabenmanager;

import org.springframework.boot.SpringApplication;

public class TestAufgabenManagerApplication {

	public static void main(String[] args) {
		SpringApplication.from(AufgabenManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

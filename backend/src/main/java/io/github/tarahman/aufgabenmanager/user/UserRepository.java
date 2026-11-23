package io.github.tarahman.aufgabenmanager.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data erzeugt die Implementierung zur Laufzeit. Abfragen ergeben sich aus dem Methodennamen,
 * {@code findByUsername} wird zu {@code ... where username = ?}.
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);

}

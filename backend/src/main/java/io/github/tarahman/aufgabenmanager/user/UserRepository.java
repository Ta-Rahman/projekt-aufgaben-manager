package io.github.tarahman.aufgabenmanager.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data erzeugt die Implementierung zur Laufzeit. Abfragen ergeben sich aus dem Methodennamen,
 * {@code findByUsername} wird zu {@code ... where username = ?}. Über {@code organisation.id} (geschrieben als
 * {@code OrganisationId}) folgt Spring Data der Beziehung; da die ID als Fremdschlüssel in {@code app_user}
 * steht, braucht die Abfrage keinen Join.
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);

	List<AppUser> findByOrganisationIdOrderByDisplayNameAsc(Long organisationId);

	Optional<AppUser> findByIdAndOrganisationId(Long id, Long organisationId);

}

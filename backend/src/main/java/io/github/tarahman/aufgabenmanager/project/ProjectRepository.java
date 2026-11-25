package io.github.tarahman.aufgabenmanager.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.tarahman.aufgabenmanager.project.ProjektDtos.Fortschritt;
import io.github.tarahman.aufgabenmanager.project.ProjektDtos.ProjektZeile;

/**
 * Abfragen für Projekte. Die Sichtbarkeitsregel (US8, D2) steht genau einmal als JPQL-Bedingung hier:
 * ADMIN sieht alle Projekte der Organisation, alle anderen nur Projekte, die sie leiten oder in denen sie
 * Mitglied sind. JPQL fragt Entitäten und ihre Felder ab, nicht Tabellen und Spalten; Hibernate übersetzt
 * das in SQL.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

	/**
	 * Wiederverwendeter Teil der Sichtbarkeitsregel. (Achtung bei Text-Blöcken: Leerzeichen am Zeilenende
	 * entfernt Java, deshalb steht die Regel beim Zusammensetzen in Klammern.) {@code exists} statt Join, damit keine Zeilen doppelt erscheinen.
	 */
	String SICHTBAR = """
			p.organisation.id = :organisationId
			and (:istAdmin = true
			     or p.lead.id = :benutzerId
			     or exists (select 1 from Project p2 join p2.members m where p2 = p and m.id = :benutzerId))
			""";

	/**
	 * Projektübersicht (P4) in einer einzigen SQL-Abfrage: Projekt, Name der Leitung und Fortschritt (US6).
	 *
	 * <p>Ohne diese Abfrage müsste man die Projekte laden und dann je Projekt Leitung und Aufgaben nachladen:
	 * 1 + 2n Abfragen (N+1-Problem). {@code left join Task t on t.project = p} verbindet Projekt und Aufgabe,
	 * obwohl {@code Project} keine Aufgabenliste hat; {@code left}, damit Projekte ohne Aufgaben mit 0/0 erscheinen.
	 */
	@Query("""
			select new io.github.tarahman.aufgabenmanager.project.ProjektDtos$ProjektZeile(
			    p.id, p.name, l.displayName, p.startDate, p.endDate, p.archivedAt,
			    sum(case when t.status = io.github.tarahman.aufgabenmanager.task.TaskStatus.ERLEDIGT then 1 else 0 end),
			    count(t))
			from Project p
			join p.lead l
			left join Task t on t.project = p
			where (""" + SICHTBAR + ")" + """
			and ((:archiviert = true and p.archivedAt is not null) or (:archiviert = false and p.archivedAt is null))
			group by p.id, p.name, l.displayName, p.startDate, p.endDate, p.archivedAt
			order by p.name
			""")
	List<ProjektZeile> uebersicht(@Param("organisationId") Long organisationId, @Param("benutzerId") Long benutzerId,
			@Param("istAdmin") boolean istAdmin, @Param("archiviert") boolean archiviert);

	/** Einzelnes Projekt nur, wenn es für den Benutzer sichtbar ist; sonst leer (wird zu 404). */
	@Query("select p from Project p where p.id = :id and " + SICHTBAR)
	Optional<Project> findeSichtbar(@Param("id") Long id, @Param("organisationId") Long organisationId,
			@Param("benutzerId") Long benutzerId, @Param("istAdmin") boolean istAdmin);

	@Query("""
			select new io.github.tarahman.aufgabenmanager.project.ProjektDtos$Fortschritt(
			    coalesce(sum(case when t.status = io.github.tarahman.aufgabenmanager.task.TaskStatus.ERLEDIGT then 1 else 0 end), 0),
			    count(t))
			from Task t
			where t.project.id = :projektId
			""")
	Fortschritt fortschritt(@Param("projektId") Long projektId);

}

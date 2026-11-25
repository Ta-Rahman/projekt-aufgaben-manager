package io.github.tarahman.aufgabenmanager.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

	/** Aufgaben eines Projekts; ersetzt eine {@code Project.tasks}-Liste (siehe Datenmodell). */
	List<Task> findByProjectIdOrderByIdAsc(Long projectId);

	/**
	 * Aufgaben für das Board mitsamt Zuständigen und Ersteller:innen in einer Abfrage.
	 * {@code join fetch} lädt die verknüpften Benutzer gleich mit, statt für jede Karte einzeln nachzuladen
	 * (sonst N+1). {@code left}, weil nicht jede Aufgabe eine zuständige Person hat.
	 */
	@Query("""
			select t from Task t
			left join fetch t.assignee
			join fetch t.createdBy
			where t.project.id = :projektId
			order by t.id
			""")
	List<Task> boardVonProjekt(@Param("projektId") Long projektId);

}

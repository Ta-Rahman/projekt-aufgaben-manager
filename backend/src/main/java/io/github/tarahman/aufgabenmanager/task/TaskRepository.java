package io.github.tarahman.aufgabenmanager.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

	/** Aufgaben eines Projekts; ersetzt eine {@code Project.tasks}-Liste (siehe Datenmodell). */
	List<Task> findByProjectIdOrderByIdAsc(Long projectId);

}

package io.github.tarahman.aufgabenmanager.task;

import java.time.Instant;
import java.time.LocalDate;

import io.github.tarahman.aufgabenmanager.common.Zeitstempel;
import io.github.tarahman.aufgabenmanager.project.Project;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aufgabe in einem Projekt. Darf hart gelöscht werden (D5).
 */
@Entity
@Table(name = "task")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Die Beziehung Projekt–Aufgabe lebt nur hier (unidirektional), siehe Klassendiagramm.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, updatable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false, updatable = false)
	private AppUser createdBy;

	/** Zuständige Person, optional. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private AppUser assignee;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status = TaskStatus.OFFEN;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Optimistisches Sperren wie bei {@link Project} (D8). */
	@Version
	@Column(nullable = false)
	private Long version;

	protected Task() {
	}

	public Task(Project project, AppUser createdBy, String title) {
		this.project = project;
		this.createdBy = createdBy;
		this.title = title;
	}

	@PrePersist
	void beimAnlegen() {
		createdAt = Zeitstempel.jetzt();
		updatedAt = createdAt;
	}

	@PreUpdate
	void beimAendern() {
		updatedAt = Zeitstempel.jetzt();
	}

	public Long getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public AppUser getCreatedBy() {
		return createdBy;
	}

	public AppUser getAssignee() {
		return assignee;
	}

	public void setAssignee(AppUser assignee) {
		this.assignee = assignee;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

}

package io.github.tarahman.aufgabenmanager.project;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OptimisticLock;

import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.Zeitstempel;
import io.github.tarahman.aufgabenmanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Projekt. Wird archiviert statt gelöscht (D5). Aufgaben hängen nicht als Liste am Projekt,
 * sondern werden über das TaskRepository geladen (siehe Datenmodell).
 */
@Entity
@Table(name = "project")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organisation_id", nullable = false, updatable = false)
	private Organisation organisation;

	/** Projektleitung; ist nicht automatisch Mitglied (D2). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lead_id", nullable = false)
	private AppUser lead;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	/** {@code null} = aktiv, sonst Zeitpunkt der Archivierung. */
	@Column(name = "archived_at")
	private Instant archivedAt;

	/**
	 * Mitglieder (D1). Die Zeilen in {@code project_member} schreibt Hibernate selbst, sobald sich die Menge
	 * ändert. {@code @ManyToMany} ist in JPA standardmäßig LAZY.
	 *
	 * <p>{@code @OptimisticLock(excluded = true)}: Hibernate würde sonst bei jeder Änderung der Mitglieder die
	 * Projektversion erhöhen. Dann bekäme jemand, der gerade Name oder Zeitraum bearbeitet, einen 409, nur weil
	 * parallel ein Mitglied hinzugefügt wurde. Mitglieder und Projektkopf werden getrennt bearbeitet (Reiter P2).
	 */
	@ManyToMany
	@OptimisticLock(excluded = true)
	@JoinTable(name = "project_member",
			joinColumns = @JoinColumn(name = "project_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<AppUser> members = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/**
	 * Optimistisches Sperren (D8): Hibernate ergänzt jedes UPDATE um {@code where version = ?} und zählt
	 * den Wert hoch. Hat inzwischen jemand anderes gespeichert, trifft das UPDATE keine Zeile, und es gibt
	 * eine Ausnahme statt eines stillschweigend überschriebenen Datensatzes.
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	protected Project() {
	}

	public Project(Organisation organisation, AppUser lead, String name) {
		this.organisation = organisation;
		this.lead = lead;
		this.name = name;
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

	public boolean isArchived() {
		return archivedAt != null;
	}

	public Long getId() {
		return id;
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public AppUser getLead() {
		return lead;
	}

	public void setLead(AppUser lead) {
		this.lead = lead;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Instant getArchivedAt() {
		return archivedAt;
	}

	public void setArchivedAt(Instant archivedAt) {
		this.archivedAt = archivedAt;
	}

	public Set<AppUser> getMembers() {
		return members;
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

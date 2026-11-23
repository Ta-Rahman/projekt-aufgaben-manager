package io.github.tarahman.aufgabenmanager.user;

import java.time.Instant;

import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.Zeitstempel;
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

/**
 * Benutzer der Anwendung. Wird nie gelöscht, sondern deaktiviert (D5, D6).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// LAZY: Die Organisation wird erst geladen, wenn jemand darauf zugreift. JPA-Standard für @ManyToOne wäre EAGER.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organisation_id", nullable = false, updatable = false)
	private Organisation organisation;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	/** Hash mit Algorithmus-Präfix, z. B. {@code {bcrypt}...}; nie das Klartextpasswort. */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	// STRING speichert den Namen der Konstante; ORDINAL würde ihre Position speichern (D4).
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AppUser() {
	}

	public AppUser(Organisation organisation, String username, String displayName, String passwordHash, Role role) {
		this.organisation = organisation;
		this.username = username;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	@PrePersist
	void beimAnlegen() {
		createdAt = Zeitstempel.jetzt();
		updatedAt = createdAt;
	}

	/** JPA-Lebenszyklus: läuft vor dem UPDATE, aber nur, wenn Hibernate eine Änderung erkannt hat. */
	@PreUpdate
	void beimAendern() {
		updatedAt = Zeitstempel.jetzt();
	}

	public Long getId() {
		return id;
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}

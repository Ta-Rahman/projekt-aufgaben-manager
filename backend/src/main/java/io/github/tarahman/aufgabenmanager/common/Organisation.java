package io.github.tarahman.aufgabenmanager.common;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Mandant. Benutzer und Projekte gehören genau einer Organisation (Mandantenfähigkeit vorbereitet, E2b).
 */
@Entity
@Table(name = "organisation")
public class Organisation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Nur für JPA: Hibernate erzeugt Objekte über einen parameterlosen Konstruktor. */
	protected Organisation() {
	}

	public Organisation(String name) {
		this.name = name;
	}

	/** JPA-Lebenszyklus: läuft unmittelbar vor dem INSERT. */
	@PrePersist
	void beimAnlegen() {
		createdAt = Zeitstempel.jetzt();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}

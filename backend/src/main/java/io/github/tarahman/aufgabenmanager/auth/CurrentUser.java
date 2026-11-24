package io.github.tarahman.aufgabenmanager.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.tarahman.aufgabenmanager.user.AppUser;
import io.github.tarahman.aufgabenmanager.user.Role;

/**
 * Der angemeldete Benutzer, wie Spring Security ihn nach dem Login in der Sitzung hält.
 *
 * <p>Bewusst keine JPA-Entität: Das Objekt liegt in der HTTP-Sitzung und überlebt damit jede Transaktion.
 * Eine Entität darin wäre außerhalb ihrer Transaktion „detached“, Lazy-Felder würden beim Zugriff scheitern.
 * Deshalb werden beim Login nur die einfachen Werte kopiert. Folge: Ändert ein Admin Rolle oder Namen,
 * gilt das für diesen Benutzer erst nach der nächsten Anmeldung.
 *
 * <p>Services bekommen ihn per {@code @AuthenticationPrincipal CurrentUser} im Controller übergeben.
 */
public final class CurrentUser implements UserDetails, CredentialsContainer {

	private final Long id;

	private final Long organisationId;

	private final String username;

	private final String displayName;

	private final Role role;

	private final boolean active;

	private String passwordHash;

	public CurrentUser(Long id, Long organisationId, String username, String displayName, Role role,
			boolean active, String passwordHash) {
		this.id = id;
		this.organisationId = organisationId;
		this.username = username;
		this.displayName = displayName;
		this.role = role;
		this.active = active;
		this.passwordHash = passwordHash;
	}

	/** Liest nur die ID der Organisation (Fremdschlüssel im Proxy), löst also kein zusätzliches SELECT aus. */
	static CurrentUser von(AppUser user) {
		return new CurrentUser(user.getId(), user.getOrganisation().getId(), user.getUsername(),
				user.getDisplayName(), user.getRole(), user.isActive(), user.getPasswordHash());
	}

	public Long getId() {
		return id;
	}

	public Long getOrganisationId() {
		return organisationId;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}

	public boolean hasRole(Role erwartet) {
		return role == erwartet;
	}

	/** Spring Security erwartet Rollen als Authorities mit Präfix {@code ROLE_}, z. B. für {@code hasRole("ADMIN")}. */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	/** Deaktivierte Benutzer können sich nicht anmelden (D6); Spring prüft das beim Login selbst. */
	@Override
	public boolean isEnabled() {
		return active;
	}

	/** Spring ruft das nach erfolgreichem Login auf: Der Hash muss nicht in der Sitzung liegen bleiben. */
	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}

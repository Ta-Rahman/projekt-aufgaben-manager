package io.github.tarahman.aufgabenmanager.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tarahman.aufgabenmanager.user.UserRepository;

/**
 * Lädt Benutzer für den Login aus der Tabelle {@code app_user} (US7).
 *
 * <p>Spring Security ruft {@link #loadUserByUsername} beim {@code POST /api/auth/login} auf, vergleicht danach
 * das eingegebene Passwort mit dem Hash ({@code PasswordEncoder}) und prüft {@code isEnabled()}.
 * Unbekannter Benutzer, falsches Passwort und deaktivierter Benutzer enden alle in 401, ohne zu verraten, was davon zutraf.
 */
@Service
class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	// Konstruktor-Injektion: Spring übergibt das Repository; kein @Autowired am Feld nötig.
	AppUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return userRepository.findByUsername(username)
			.map(CurrentUser::von)
			.orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer"));
	}

}

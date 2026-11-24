package io.github.tarahman.aufgabenmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.tarahman.aufgabenmanager.auth.CurrentUser;
import io.github.tarahman.aufgabenmanager.common.KonfliktException;
import io.github.tarahman.aufgabenmanager.common.NichtGefundenException;
import io.github.tarahman.aufgabenmanager.common.Organisation;
import io.github.tarahman.aufgabenmanager.common.OrganisationRepository;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAendern;
import io.github.tarahman.aufgabenmanager.user.BenutzerDtos.BenutzerAnlegen;

/**
 * Reiner Unit-Test der Regeln im {@link UserService}, ohne Spring und ohne Datenbank.
 *
 * <p>Mockito ersetzt die Abhängigkeiten durch Attrappen: {@code given(...)} legt fest, was sie liefern,
 * {@code then(...)} prüft, ob und wie sie aufgerufen wurden. Läuft in Millisekunden und zeigt genau,
 * welche Regel bricht.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	OrganisationRepository organisationRepository;

	@Mock
	PasswordEncoder passwordEncoder;

	@InjectMocks
	UserService userService;

	final CurrentUser admin = new CurrentUser(1L, 10L, "admin", "Anna Admin", Role.ADMIN, true, null);

	@Test
	void anlegenMitVergebenemNamenSpeichertNichts() {
		given(userRepository.existsByUsername("leitung")).willReturn(true);

		assertThatThrownBy(() -> userService.anlegen(
				new BenutzerAnlegen("leitung", "Doppelt", Role.MITARBEITER, "geheim123"), admin))
			.isInstanceOf(KonfliktException.class);
		then(userRepository).should(never()).save(any());
	}

	@Test
	void anlegenSpeichertNurDenHash() {
		given(passwordEncoder.encode("geheim123")).willReturn("{bcrypt}HASH");
		given(organisationRepository.getReferenceById(10L)).willReturn(new Organisation("Testfirma"));
		given(userRepository.save(any())).willAnswer(aufruf -> aufruf.getArgument(0));

		userService.anlegen(new BenutzerAnlegen("neu", "  Neue Person ", Role.MITARBEITER, "geheim123"), admin);

		then(userRepository).should().save(org.mockito.ArgumentMatchers.argThat(u ->
				u.getPasswordHash().equals("{bcrypt}HASH") && u.getDisplayName().equals("Neue Person")));
	}

	@Test
	void eigenesKontoDeaktivierenIstVerboten() {
		AppUser selbst = new AppUser(new Organisation("Testfirma"), "admin", "Anna Admin", "{noop}x", Role.ADMIN);
		given(userRepository.findByIdAndOrganisationId(1L, 10L)).willReturn(Optional.of(mitId(selbst, 1L)));

		assertThatThrownBy(() -> userService.aendern(1L, new BenutzerAendern("Anna Admin", Role.ADMIN, false, null), admin))
			.isInstanceOf(KonfliktException.class);
		assertThatThrownBy(() -> userService.aendern(1L, new BenutzerAendern("Anna Admin", Role.MITARBEITER, true, null), admin))
			.isInstanceOf(KonfliktException.class);
		assertThat(selbst.isActive()).isTrue();
		assertThat(selbst.getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void aendernOhnePasswortBehaeltDenHash() {
		AppUser user = mitId(new AppUser(new Organisation("Testfirma"), "ma", "Max", "{bcrypt}ALT", Role.MITARBEITER), 5L);
		given(userRepository.findByIdAndOrganisationId(5L, 10L)).willReturn(Optional.of(user));

		userService.aendern(5L, new BenutzerAendern("Max Neu", Role.PROJEKTLEITER, true, null), admin);

		assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}ALT");
		assertThat(user.getRole()).isEqualTo(Role.PROJEKTLEITER);
		then(passwordEncoder).shouldHaveNoInteractions();
	}

	@Test
	void fremdeOderUnbekannteIdIstNichtGefunden() {
		given(userRepository.findByIdAndOrganisationId(99L, 10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.aendern(99L, new BenutzerAendern("X", Role.MITARBEITER, true, null), admin))
			.isInstanceOf(NichtGefundenException.class);
	}

	/** Die ID vergibt sonst die Datenbank; im Unit-Test setzen wir sie per Reflection. */
	private static AppUser mitId(AppUser user, Long id) {
		org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}

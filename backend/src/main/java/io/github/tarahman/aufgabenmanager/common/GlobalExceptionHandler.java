package io.github.tarahman.aufgabenmanager.common;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Einheitliches Fehlerformat für die ganze API nach RFC 9457 („Problem Details“), z. B.:
 *
 * <pre>
 * { "type": "about:blank", "title": "Conflict", "status": 409,
 *   "detail": "Benutzername ist bereits vergeben", "instance": "/api/users" }
 * </pre>
 *
 * <p>Die Basisklasse {@link ResponseEntityExceptionHandler} übersetzt schon alle Spring-MVC-Fehler
 * (falsches JSON, falsche Methode, ...) in dieses Format. Hier kommen die eigenen fachlichen Ausnahmen dazu.
 *
 * <p>Bewusst kein Auffangen von {@code Exception} oder {@code AccessDeniedException}: Fehlende Rechte
 * soll weiterhin Spring Security beantworten (403), unerwartete Fehler Spring Boot (500 ohne Interna).
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NichtGefundenException.class)
	ProblemDetail nichtGefunden(NichtGefundenException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(KonfliktException.class)
	ProblemDetail konflikt(KonfliktException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(UngueltigeAnfrageException.class)
	ProblemDetail ungueltig(UngueltigeAnfrageException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/** {@code @Version} hat ein verlorenes Update verhindert (D8). */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail gleichzeitigGeaendert(ObjectOptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Der Datensatz wurde inzwischen von jemand anderem geändert. Bitte neu laden.");
	}

	/**
	 * Bean Validation an Request-DTOs ({@code @Valid}) ist fehlgeschlagen: 400 mit Liste der Feldfehler,
	 * damit das Frontend die Meldung direkt am Eingabefeld anzeigen kann.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Eingaben sind ungültig.");
		List<Feldfehler> fehler = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(f -> new Feldfehler(f.getField(), f.getDefaultMessage()))
			.toList();
		problem.setProperty("fehler", fehler);
		return ResponseEntity.badRequest().body(problem);
	}

	record Feldfehler(String feld, String meldung) {
	}

}

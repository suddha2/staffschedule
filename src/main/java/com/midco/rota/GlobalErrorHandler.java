package com.midco.rota;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handling for the REST API.
 *
 * <p>Replaces the older CsvErrorHandler that caught {@code Exception.class}
 * indiscriminately — which (a) intercepted every {@link ResponseStatusException}
 * thrown by the controllers and turned it into a misleading 500 prefixed with
 * "An error occurred while generating the CSV.", and (b) leaked raw exception
 * text to clients.
 *
 * <p>This handler:
 * <ul>
 *   <li>Preserves {@code ResponseStatusException}'s status and reason — so
 *       endpoints throwing e.g. {@code 401 "Incorrect code."} reach the
 *       client unchanged.</li>
 *   <li>Returns 400 with field-level details on {@code @Valid} failures.</li>
 *   <li>Returns a clean generic 500 for anything truly unexpected, logging
 *       the full stack trace server-side instead of returning it to the
 *       caller.</li>
 * </ul>
 *
 * <p>Response shape is always {@code {status, error, message}} for the
 * client to read consistently.
 */
@ControllerAdvice
public class GlobalErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
		int status = ex.getStatusCode().value();
		return ResponseEntity.status(status).body(body(status, ex.getReason()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest()
				.body(body(HttpStatus.BAD_REQUEST.value(),
						message.isEmpty() ? "Validation failed" : message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
		// Full stack trace stays server-side; the client gets the exception
		// message so download/streaming endpoints don't die silently. The old
		// CsvErrorHandler was added for exactly this reason — we keep that
		// visibility but lose the misleading "CSV" prefix and the
		// status-code clobbering that came with it.
		log.error("Unhandled exception", ex);
		String message = (ex.getMessage() == null || ex.getMessage().isBlank())
				? ex.getClass().getSimpleName()
				: ex.getMessage();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(body(HttpStatus.INTERNAL_SERVER_ERROR.value(), message));
	}

	private static Map<String, Object> body(int status, String message) {
		// LinkedHashMap so the JSON keys come out in a predictable order.
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status);
		try {
			body.put("error", HttpStatus.valueOf(status).getReasonPhrase());
		} catch (IllegalArgumentException e) {
			body.put("error", "Error");
		}
		if (message != null && !message.isEmpty()) {
			body.put("message", message);
		}
		return body;
	}
}

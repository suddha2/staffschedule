package com.midco.rota.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.model.Role;
import com.midco.rota.model.User;
import com.midco.rota.repository.UserRepository;
import com.midco.rota.service.MicrosoftIdTokenValidator;
import com.midco.rota.service.PasetoTokenService;

/**
 * "Sign in with Microsoft" endpoint. The browser obtains an ID token from
 * Microsoft using MSAL.js with PKCE and POSTs it here; we validate it,
 * look up the matching user by email (= users.username), and issue our
 * normal PASETO session token. From that point on the session is identical
 * to a password sign-in.
 *
 * <p>Users are NOT auto-provisioned — if no row exists for that email the
 * caller gets 403 and an administrator must create the user first.
 */
@RestController
@RequestMapping("/api/auth/microsoft")
public class MicrosoftAuthController {

	private static final Logger log = LoggerFactory.getLogger(MicrosoftAuthController.class);

	private final MicrosoftIdTokenValidator validator;
	private final UserRepository userRepository;
	private final PasetoTokenService pasetoTokenService;

	public MicrosoftAuthController(MicrosoftIdTokenValidator validator,
			UserRepository userRepository, PasetoTokenService pasetoTokenService) {
		this.validator = validator;
		this.userRepository = userRepository;
		this.pasetoTokenService = pasetoTokenService;
	}

	@PostMapping
	public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
		if (!validator.isConfigured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Microsoft login is not configured");
		}
		String idToken = body == null ? null : body.get("idToken");
		if (idToken == null || idToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idToken is required");
		}

		Jwt jwt;
		try {
			jwt = validator.validate(idToken);
		} catch (Exception e) {
			log.info("Microsoft ID token rejected: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Microsoft sign-in failed");
		}

		String email = firstNonBlank(
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("upn"));
		if (email == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Microsoft token did not include an email claim");
		}
		String normalised = email.trim().toLowerCase();

		User user = userRepository.findByUsername(normalised)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
						"No account for this Microsoft user. Contact an administrator."));

		if (!user.isActive()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive.");
		}

		Set<String> roleNames = user.getRoles().stream()
				.map(Role::getName)
				.collect(Collectors.toSet());
		String token = pasetoTokenService.generateToken(user.getUsername(), roleNames);
		log.info("Issued PASETO for Microsoft sign-in: user={} roles={}", user.getUsername(), roleNames);
		return ResponseEntity.ok(Map.of("token", token));
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && !v.isBlank()) return v;
		}
		return null;
	}
}

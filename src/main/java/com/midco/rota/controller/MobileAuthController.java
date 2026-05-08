package com.midco.rota.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.midco.rota.dto.GoogleAuthRequestDTO;
import com.midco.rota.dto.MobileAuthResponseDTO;
import com.midco.rota.model.Employee;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.PasetoTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

	private static final Logger log = LoggerFactory.getLogger(MobileAuthController.class);

	private final EmployeeRepository employeeRepository;
	private final PasetoTokenService pasetoTokenService;

	public MobileAuthController(EmployeeRepository employeeRepository, PasetoTokenService pasetoTokenService) {
		this.employeeRepository = employeeRepository;
		this.pasetoTokenService = pasetoTokenService;
	}

	@PostMapping("/google")
	public ResponseEntity<MobileAuthResponseDTO> googleSignIn(@RequestBody @Valid GoogleAuthRequestDTO dto) {
		if (FirebaseApp.getApps().isEmpty()) {
			log.error("Google sign-in attempted but FirebaseApp is not initialized");
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Authentication service unavailable");
		}

		FirebaseToken decoded;
		try {
			decoded = FirebaseAuth.getInstance().verifyIdToken(dto.getIdToken());
		} catch (FirebaseAuthException e) {
			log.warn("Firebase ID token verification failed: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
		}

		if (!decoded.isEmailVerified()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account email not verified");
		}

		String email = decoded.getEmail();
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no email claim");
		}

		Employee employee = employeeRepository.findByEmail(email)
				.filter(Employee::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No active employee for this Google account"));

		String token = pasetoTokenService.generateToken(email, Set.of("ROLE_EMPLOYEE"));
		log.info("Issued mobile PASETO for employee {} ({})", employee.getId(), email);

		return ResponseEntity.ok(new MobileAuthResponseDTO(
				token, employee.getId(), employee.getFirstName(), employee.getLastName()));
	}
}

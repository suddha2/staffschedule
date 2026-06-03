package com.midco.rota.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.model.LoginRequest;
import com.midco.rota.model.Role;
import com.midco.rota.model.User;
import com.midco.rota.repository.UserRepository;
import com.midco.rota.service.AuthService;
import com.midco.rota.service.PasetoTokenService;

@RestController
public class AuthController {
	private final PasetoTokenService tokenService;
	private final AuthService authService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthController(AuthService authService, PasetoTokenService tokenService, UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.authService = authService;
		this.tokenService = tokenService;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@PostMapping("/api/auth/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {

		User user = userRepository.findByUsername(request.getUserName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		if (!passwordEncoder.matches(request.getPass(), user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		// Soft-deleted / disabled users can't sign in even with the right password.
		if (!user.isActive()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive.");
		}

		Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
		String token = tokenService.generateToken(user.getUsername(), roleNames);
		return ResponseEntity.ok(Map.of("token", token));

	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/api/auth/register")
	public ResponseEntity<?> register(@RequestBody LoginRequest request) {
		User user = new User();
		user.setUsername(request.getUserName());
		user.setPassword(passwordEncoder.encode(request.getPass()));
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());
		userRepository.save(user);
		return ResponseEntity.ok("User registered");
	}

	@GetMapping("/api/auth/me")
	public ResponseEntity<?> loggedUserInfo(Authentication auth) {
		Optional<User> user = userRepository.findByUsername(auth.getName());
		if (user.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		Map<String, Object> result = new HashMap<>();
		result.put("userName", user.get().getUsername());
		// Return just the role names; the frontend doesn't need the Role entity shape.
		result.put("roles", user.get().getRoles().stream()
				.map(Role::getName)
				.collect(Collectors.toSet()));
		return ResponseEntity.ok(result);
	}
}

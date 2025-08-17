package com.midco.rota.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.model.LoginRequest;
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

	@PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
//        // Validate credentials (e.g., via UserDetailsService)
//        if (isValidUser(request.getUserName(), request.getPass())) {
//            String token = tokenService.generateToken(request.getUserName());
//            return ResponseEntity.ok(Map.of("token", token));
//        }
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//    }

	public ResponseEntity<?> login(@RequestBody LoginRequest request) {

		User user = userRepository.findByUsername(request.getUserName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		if (!passwordEncoder.matches(request.getPass(), user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		String token = tokenService.generateToken(user.getUsername(), user.getRoles());
		return ResponseEntity.ok(Map.of("token", token));

	}

	@PreAuthorize("hasRole('admin')")
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody LoginRequest request) {
		User user = new User();
		user.setUsername(request.getUserName());
		user.setPassword(passwordEncoder.encode(request.getPass()));
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());
		userRepository.save(user);
		return ResponseEntity.ok("User registered");
	}
}

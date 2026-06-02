package com.midco.rota.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.dto.PasswordResetRequest;
import com.midco.rota.dto.UserCreateRequest;
import com.midco.rota.dto.UserResponseDTO;
import com.midco.rota.dto.UserUpdateRequest;
import com.midco.rota.model.Role;
import com.midco.rota.model.User;
import com.midco.rota.repository.RoleRepository;
import com.midco.rota.repository.UserRepository;

/**
 * Admin-only user management. Sits under /api/admin/** so the existing
 * path matcher already gates the namespace to ADMIN — the class-level
 * @PreAuthorize here is defence in depth so the policy is also visible at
 * the call site.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
public class UserController {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserController(UserRepository userRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public ResponseEntity<List<UserResponseDTO>> list() {
		List<UserResponseDTO> out = userRepository.findAll().stream()
				.map(UserResponseDTO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(out);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDTO> get(@PathVariable Long id) {
		return userRepository.findById(id)
				.map(u -> ResponseEntity.ok(UserResponseDTO.fromEntity(u)))
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<UserResponseDTO> create(@RequestBody UserCreateRequest req, Authentication auth) {
		String username = trimRequired(req.getUsername(), "username");
		String password = req.getPassword();
		if (password == null || password.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
		}
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that username already exists.");
		}

		Set<Role> resolvedRoles = resolveRoles(req.getRoles());
		// Privilege-escalation guard: a non-admin caller cannot grant the ADMIN role.
		requireAdminToGrantAdmin(auth, resolvedRoles);

		User u = new User();
		u.setUsername(username);
		u.setPassword(passwordEncoder.encode(password));
		u.setActive(req.getActive() == null ? true : req.getActive());
		u.setCreatedAt(LocalDateTime.now());
		u.setRoles(resolvedRoles);

		try {
			User saved = userRepository.save(u);
			return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(saved));
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"A user with that username already exists.");
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponseDTO> update(@PathVariable Long id, @RequestBody UserUpdateRequest req,
			Authentication auth) {
		User u = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Privilege-escalation guard: non-admins can't touch an ADMIN user at all.
		requireAdminToTouchAdminTarget(auth, u, "edit");

		if (req.getUsername() != null) {
			String newName = req.getUsername().trim();
			if (newName.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username cannot be blank");
			}
			if (!newName.equalsIgnoreCase(u.getUsername())
					&& userRepository.existsByUsernameIgnoreCase(newName)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"A user with that username already exists.");
			}
			u.setUsername(newName);
		}
		if (req.getActive() != null) {
			u.setActive(req.getActive());
		}
		if (req.getRoles() != null) {
			Set<Role> resolvedRoles = resolveRoles(req.getRoles());
			// Privilege-escalation guard: a non-admin can't promote anyone to ADMIN.
			requireAdminToGrantAdmin(auth, resolvedRoles);
			u.setRoles(resolvedRoles);
		}

		// Lockout guard: editing active or roles must not strip the system of
		// its last active ADMIN.
		ensureSystemRetainsActiveAdmin(u);

		try {
			User saved = userRepository.save(u);
			return ResponseEntity.ok(UserResponseDTO.fromEntity(saved));
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"A user with that username already exists.");
		}
	}

	@PutMapping("/{id}/password")
	public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordResetRequest req,
			Authentication auth) {
		if (req == null || req.getPassword() == null || req.getPassword().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
		}
		User u = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		// Privilege-escalation guard: only ADMIN can reset an ADMIN's password.
		requireAdminToTouchAdminTarget(auth, u, "reset the password of");
		u.setPassword(passwordEncoder.encode(req.getPassword()));
		userRepository.save(u);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Soft delete: flip the active flag off rather than hard-deleting. Keeps
	 * any audit/foreign-key references intact and lets the same username be
	 * reactivated later. Trying to delete the only remaining active ADMIN is
	 * rejected to avoid a lockout.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable Long id, Authentication auth) {
		User u = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Privilege-escalation guard: only ADMIN can deactivate an ADMIN.
		requireAdminToTouchAdminTarget(auth, u, "deactivate");

		if (!u.isActive()) {
			return ResponseEntity.noContent().build();
		}
		u.setActive(false);
		// Lockout guard: the system must always retain at least one active ADMIN.
		ensureSystemRetainsActiveAdmin(u);
		userRepository.save(u);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Throws 409 if applying the current in-memory state of {@code u} would
	 * leave the system with zero active ADMIN users. Call this after any
	 * mutation to active/roles and before save.
	 */
	private void ensureSystemRetainsActiveAdmin(User u) {
		boolean stillActiveAdmin = u.isActive() && u.getRoles() != null
				&& u.getRoles().stream().map(Role::getName).anyMatch("ADMIN"::equalsIgnoreCase);
		if (stillActiveAdmin) {
			return; // this user still covers the invariant
		}
		if (countOtherActiveAdmins(u.getId()) == 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"At least one active ADMIN user must remain.");
		}
	}

	private long countOtherActiveAdmins(Long excludeUserId) {
		return userRepository.findAll().stream()
				.filter(other -> !other.getId().equals(excludeUserId))
				.filter(User::isActive)
				.filter(other -> other.getRoles() != null
						&& other.getRoles().stream().map(Role::getName)
								.anyMatch("ADMIN"::equalsIgnoreCase))
				.count();
	}

	/**
	 * Resolves role names to managed Role entities. Unknown names are rejected
	 * with 400 (caller must use one of the seeded roles). At least one role is
	 * required so the user can actually do something after login.
	 */
	private Set<Role> resolveRoles(List<String> names) {
		if (names == null || names.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"At least one role is required.");
		}
		Set<String> wanted = names.stream()
				.filter(Optional::ofNullable)
				.filter(n -> n != null && !n.isBlank())
				.map(String::trim)
				.map(String::toUpperCase)
				.collect(Collectors.toSet());
		if (wanted.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"At least one role is required.");
		}
		List<Role> found = roleRepository.findByNameIn(wanted);
		if (found.size() != wanted.size()) {
			Set<String> foundNames = found.stream().map(Role::getName)
					.collect(Collectors.toSet());
			Set<String> missing = new HashSet<>(wanted);
			missing.removeAll(foundNames);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unknown role(s): " + String.join(", ", missing));
		}
		return new HashSet<>(found);
	}

	private static boolean callerIsAdmin(Authentication auth) {
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

	private static boolean userHasAdminRole(User u) {
		return u.getRoles() != null
				&& u.getRoles().stream().map(Role::getName).anyMatch("ADMIN"::equalsIgnoreCase);
	}

	/**
	 * Non-admins (i.e. OPS_MANAGER) must not touch users that currently have
	 * the ADMIN role at all — edit, password reset or deactivate. Stops them
	 * taking over an admin account.
	 */
	private static void requireAdminToTouchAdminTarget(Authentication auth, User target, String action) {
		if (callerIsAdmin(auth)) return;
		if (userHasAdminRole(target)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only ADMIN can " + action + " an ADMIN user.");
		}
	}

	/**
	 * Non-admins (i.e. OPS_MANAGER) must not grant the ADMIN role to anyone.
	 * Otherwise they could create an admin user and use it to take over.
	 */
	private static void requireAdminToGrantAdmin(Authentication auth, Set<Role> requestedRoles) {
		if (callerIsAdmin(auth)) return;
		boolean wantsAdmin = requestedRoles.stream().map(Role::getName)
				.anyMatch("ADMIN"::equalsIgnoreCase);
		if (wantsAdmin) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only ADMIN can grant the ADMIN role.");
		}
	}

	private static String trimRequired(String s, String field) {
		if (s == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
		}
		String t = s.trim();
		if (t.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
		}
		return t;
	}
}

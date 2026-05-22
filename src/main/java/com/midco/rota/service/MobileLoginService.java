package com.midco.rota.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.dto.MobileAuthResponseDTO;
import com.midco.rota.model.Employee;
import com.midco.rota.model.MobileLoginCode;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.MobileLoginCodeRepository;

/**
 * Email one-time-code sign-in for the mobile app. The employee requests a code
 * to their email and verifies it to receive a PASETO session token — no
 * password, works for any email provider.
 */
@Service
public class MobileLoginService {

	private static final Logger log = LoggerFactory.getLogger(MobileLoginService.class);

	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int MAX_VERIFY_ATTEMPTS = 5;
	private static final int MAX_CODES_PER_HOUR = 5;
	private static final Duration SESSION_TTL = Duration.ofDays(30);

	private final SecureRandom random = new SecureRandom();

	private final EmployeeRepository employeeRepository;
	private final MobileLoginCodeRepository codeRepository;
	private final PasetoTokenService pasetoTokenService;
	private final PasswordEncoder passwordEncoder;
	private final GraphMailService graphMailService;

	// Google Play review bypass. When BOTH are set, signing in with this exact
	// email accepts the fixed code below — no email is sent and no code is stored —
	// so store reviewers can get past OTP login. Both unset (the default) disables
	// it entirely. Set only in the external prod config, never committed.
	@Value("${mobile.review.email:}")
	private String reviewEmail;

	@Value("${mobile.review.code:}")
	private String reviewCode;

	public MobileLoginService(EmployeeRepository employeeRepository,
			MobileLoginCodeRepository codeRepository, PasetoTokenService pasetoTokenService,
			PasswordEncoder passwordEncoder, GraphMailService graphMailService) {
		this.employeeRepository = employeeRepository;
		this.codeRepository = codeRepository;
		this.pasetoTokenService = pasetoTokenService;
		this.passwordEncoder = passwordEncoder;
		this.graphMailService = graphMailService;
	}

	/**
	 * Generates and emails a login code for the given email. Completes without
	 * error even when the email matches no employee — the caller must not reveal
	 * whether an account exists (prevents account enumeration).
	 */
	@Transactional
	public void requestCode(String rawEmail) {
		String email = normalise(rawEmail);

		// Play review account: no code is generated or emailed — verifyCode accepts
		// the fixed review code directly.
		if (isReviewLogin(email)) {
			log.info("Login code requested for Play review account {} — no-op", email);
			return;
		}

		// Rate-limit: cap how many codes one address can request per hour.
		long recent = codeRepository.countByEmailAndCreatedAtAfter(
				email, LocalDateTime.now().minusHours(1));
		if (recent >= MAX_CODES_PER_HOUR) {
			log.warn("Login-code rate limit hit for {}", email);
			return;
		}

		Employee employee = employeeRepository.findByEmail(email)
				.filter(Employee::isActive)
				.orElse(null);
		if (employee == null) {
			// No active employee for this email — do nothing, but don't reveal that.
			log.info("Login code requested for unknown/inactive email {}", email);
			return;
		}

		String code = generateCode();
		MobileLoginCode entry = new MobileLoginCode();
		entry.setEmail(email);
		entry.setCodeHash(passwordEncoder.encode(code));
		entry.setExpiresAt(LocalDateTime.now().plus(CODE_TTL));
		codeRepository.save(entry);

		try {
			deliverCode(email, code);
		} catch (EmailSendException e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Couldn't send the code right now — please try again shortly.");
		}
	}

	/**
	 * Verifies a code and, on success, issues a PASETO session token. Throws 401
	 * for an invalid / expired / exhausted code.
	 *
	 * <p>Deliberately NOT {@code @Transactional}: a wrong-code attempt bumps the
	 * attempt counter and then throws, and a surrounding transaction would roll
	 * that increment back — defeating the brute-force limit. Each repository
	 * save here commits on its own.
	 */
	public MobileAuthResponseDTO verifyCode(String rawEmail, String code) {
		String email = normalise(rawEmail);

		// Play review account: accept the fixed code without a stored entry, then
		// issue a session token for the (active) review employee like any other login.
		if (isReviewLogin(email)) {
			if (!reviewCode.equals(code)) {
				throw unauthorized("Incorrect code.");
			}
			Employee reviewer = employeeRepository.findByEmail(email)
					.filter(Employee::isActive)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"No active employee for this email."));
			String token = pasetoTokenService.generateToken(
					email, Set.of("ROLE_EMPLOYEE"), SESSION_TTL);
			log.info("Issued mobile PASETO for Play review account {} (employee {})",
					email, reviewer.getId());
			return new MobileAuthResponseDTO(token, reviewer.getId(),
					reviewer.getFirstName(), reviewer.getLastName());
		}

		MobileLoginCode entry = codeRepository
				.findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
				.orElseThrow(() -> unauthorized("No code was requested for this email."));

		if (LocalDateTime.now().isAfter(entry.getExpiresAt())) {
			throw unauthorized("This code has expired — request a new one.");
		}
		if (entry.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
			throw unauthorized("Too many incorrect attempts — request a new code.");
		}
		if (!passwordEncoder.matches(code, entry.getCodeHash())) {
			entry.setAttempts(entry.getAttempts() + 1);
			codeRepository.save(entry);
			throw unauthorized("Incorrect code.");
		}

		entry.setConsumed(true);
		codeRepository.save(entry);

		Employee employee = employeeRepository.findByEmail(email)
				.filter(Employee::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No active employee for this email."));

		String token = pasetoTokenService.generateToken(
				email, Set.of("ROLE_EMPLOYEE"), SESSION_TTL);
		log.info("Issued mobile PASETO for employee {} ({}) via email code", employee.getId(), email);

		return new MobileAuthResponseDTO(token, employee.getId(),
				employee.getFirstName(), employee.getLastName());
	}

	private void deliverCode(String email, String code) {
		String subject = "Your Staff Schedule sign-in code";
		String body = "<p>Your sign-in code is:</p>"
				+ "<p style=\"font-size:24px;font-weight:bold;letter-spacing:3px\">" + code + "</p>"
				+ "<p>It expires in 10 minutes. If you didn't request this, you can ignore this email.</p>";
		if (graphMailService.isConfigured()) {
			graphMailService.sendHtmlEmail(email, subject, body);
		} else {
			// Email isn't wired up yet — log the code so the flow can still be
			// tested end-to-end. Not reached once GraphMailService is configured.
			log.warn("[DEV] Email not configured — login code for {} is: {}", email, code);
		}
	}

	private String generateCode() {
		return String.format("%06d", random.nextInt(1_000_000));
	}

	/** True only when the review bypass is fully configured and this is that account. */
	private boolean isReviewLogin(String normalisedEmail) {
		return reviewEmail != null && !reviewEmail.isBlank()
				&& reviewCode != null && !reviewCode.isBlank()
				&& normalisedEmail.equals(normalise(reviewEmail));
	}

	private static String normalise(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	private static ResponseStatusException unauthorized(String message) {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
	}
}

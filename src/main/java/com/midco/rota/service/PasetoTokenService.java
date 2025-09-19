package com.midco.rota.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.midco.rota.TokenValidationException;

import dev.paseto.jpaseto.ExpiredPasetoException;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoException;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.Pasetos;

@Service
public class PasetoTokenService {

	@Value("${paseto.issuer}")
	private String issuer;

	@Value("${paseto.audience}")
	private String audience;

	@Value("${paseto.public-key}")
	private String publicKeyPath;

	@Value("${paseto.private-key}")
	private String privateKeyPath;

	// ============================
	// Generate Token (asymmetric)
	// ============================
	public String generateToken(String username, Set<String> roles) {
		Instant now = Instant.now();

		try {
			PrivateKey privateKey = loadPrivateKey();
			return Pasetos.V2.PUBLIC.builder().setPrivateKey(privateKey).setIssuedAt(now)
					.setExpiration(now.plus(Duration.ofHours(2))).setSubject(username).setIssuer(issuer)
					.setAudience(audience).claim("roles", roles).compact();

		} catch (Exception e) {
			throw new RuntimeException("Failed to generate token", e);
		}
	}

	// ============================
	// Validate Token
	// ============================
	public Optional<String> validateToken(String token) {
		try {
			PublicKey publicKey = loadPublicKey();
			PasetoParser parser = Pasetos.parserBuilder().setPublicKey(publicKey).build();

			Paseto parsed = parser.parse(token);
			return Optional.ofNullable(parsed.getClaims().getSubject());

		} catch (PasetoException e) {
			throw new TokenValidationException("Token validation failed", e);
		} catch (Exception e) {
			throw new TokenValidationException("Unexpected error during token validation", e);
		}
	}

	// ============================
	// Key loaders (flexible)
	// ============================
	private PrivateKey loadPrivateKey() throws Exception {
		byte[] decoded = loadKeyBytes(privateKeyPath);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		return KeyFactory.getInstance("Ed25519").generatePrivate(spec);
	}

	private PublicKey loadPublicKey() throws Exception {
		byte[] decoded = loadKeyBytes(publicKeyPath);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
		return KeyFactory.getInstance("Ed25519").generatePublic(spec);
	}

	private byte[] loadKeyBytes(String path) throws IOException {
		Resource resource = new FileSystemResource(path);
		if (!resource.exists()) {
			resource = new ClassPathResource(path);
		}
		byte[] raw = resource.getInputStream().readAllBytes();
		return Base64.getDecoder().decode(raw);
	}
}

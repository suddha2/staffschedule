package com.midco.rota.service;

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
import org.springframework.stereotype.Service;

import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.Pasetos;

@Service
public class PasetoTokenService {

	private final SecretKey secretKey;
	
	 @Value("${paseto.issuer}")
	    private String issuer;

	    @Value("${paseto.audience}")
	    private String audience;

	public PasetoTokenService() {
		byte[] keyBytes = new byte[32]; // 256-bit key for v2.local
		new SecureRandom().nextBytes(keyBytes);
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
	}

	public String generateToken(String username,Set<String> roles) {
		Instant now = Instant.now();
		String token = "";

		try {


			ClassPathResource resource = new ClassPathResource("private.key");

			try (InputStream inputStream = resource.getInputStream()) {
				byte[] rawBytes = inputStream.readAllBytes();
				byte[] privBytes = Base64.getDecoder().decode(rawBytes);

				PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
				PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(privSpec);

				token = Pasetos.V2.PUBLIC.builder().setPrivateKey(privateKey) // ✅ asymmetric signing
						.setIssuedAt(now).setExpiration(now.plus(Duration.ofHours(2))).setSubject(username)
						 .setIssuer(issuer).setAudience(audience).claim("roles", roles).compact();
				
				System.out.println(this.validateToken(token));
				return token;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return token;

	}

	public Optional<String> validateToken(String token) {
		
			ClassPathResource resource = new ClassPathResource("public.key");

			try (InputStream inputStream = resource.getInputStream()) {
				byte[] rawBytes = inputStream.readAllBytes();
				byte[] pubBytes = Base64.getDecoder().decode(rawBytes);
				
			//byte[] pubBytes = Base64.getDecoder().decode(Files.readAllBytes(Paths.get("public.key")));
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
			PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(pubSpec);
			
			PasetoParser parser = Pasetos.parserBuilder().setPublicKey(publicKey).setSharedSecret(secretKey).build();

			Paseto parsed = parser.parse(token);
			
			System.out.println(parsed.getClaims());
			return Optional.ofNullable(parsed.getClaims().getSubject());


		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}

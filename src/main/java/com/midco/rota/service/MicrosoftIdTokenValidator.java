package com.midco.rota.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;

/**
 * Validates Microsoft Entra (Azure AD) v2.0 ID tokens for the "Sign in with
 * Microsoft" flow. Uses Spring's NimbusJwtDecoder, which fetches the tenant's
 * JWKS from OIDC discovery and verifies the signature, issuer and expiry. We
 * additionally check the audience matches our configured client id.
 *
 * <p>The validator is "configured" only when both
 * {@code microsoft.tenant-id} and {@code microsoft.client-id} are set. While
 * unset, callers should report 503 ("Microsoft login is not configured").
 */
@Service
public class MicrosoftIdTokenValidator {

	private static final Logger log = LoggerFactory.getLogger(MicrosoftIdTokenValidator.class);

	@Value("${microsoft.tenant-id:}")
	private String tenantId;

	@Value("${microsoft.client-id:}")
	private String clientId;

	private volatile JwtDecoder decoder;

	public boolean isConfigured() {
		return tenantId != null && !tenantId.isBlank()
				&& clientId != null && !clientId.isBlank();
	}

	/**
	 * Decode + verify an ID token, throwing if anything fails. Returns the
	 * verified Jwt with claims. Caller should then read the email-like claim
	 * (email / preferred_username / upn) for user lookup.
	 */
	public Jwt validate(String idToken) {
		if (!isConfigured()) {
			throw new IllegalStateException("Microsoft login is not configured");
		}
		JwtDecoder d = decoder;
		if (d == null) {
			synchronized (this) {
				if (decoder == null) {
					String issuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
					log.info("Initialising Microsoft JwtDecoder for issuer {}", issuer);
					decoder = JwtDecoders.fromIssuerLocation(issuer);
				}
				d = decoder;
			}
		}
		Jwt jwt = d.decode(idToken);   // throws on bad sig, wrong issuer, expiry, etc.
		if (jwt.getAudience() == null || !jwt.getAudience().contains(clientId)) {
			throw new IllegalArgumentException("ID token audience does not match this application");
		}
		return jwt;
	}
}

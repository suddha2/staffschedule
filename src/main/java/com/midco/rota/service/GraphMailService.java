package com.midco.rota.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Sends transactional email (login one-time codes) through Microsoft Graph
 * using an app-only — client-credentials — Azure AD app registration.
 *
 * <p>The four {@code graph.mail.*} properties come from the Azure app
 * registration set up by the M365 vendor: tenant id, client id, client secret,
 * and the mailbox to send as. The secret must be supplied via an environment
 * variable, never committed.
 */
@Service
public class GraphMailService {

	private static final Logger log = LoggerFactory.getLogger(GraphMailService.class);

	private static final String TOKEN_URL =
			"https://login.microsoftonline.com/%s/oauth2/v2.0/token";
	private static final String SENDMAIL_URL =
			"https://graph.microsoft.com/v1.0/users/%s/sendMail";

	@Value("${graph.mail.tenant-id:}")
	private String tenantId;

	@Value("${graph.mail.client-id:}")
	private String clientId;

	@Value("${graph.mail.client-secret:}")
	private String clientSecret;

	@Value("${graph.mail.sender:}")
	private String sender;

	private final RestTemplate rest = new RestTemplate();

	// Cached app-only access token; refreshed shortly before it expires.
	private volatile String cachedToken;
	private volatile Instant tokenExpiry = Instant.EPOCH;

	@PostConstruct
	void logConfig() {
		if (isConfigured()) {
			log.info("GraphMailService configured; sending as {}", sender);
		} else {
			log.warn("GraphMailService is NOT configured (graph.mail.* properties missing). "
					+ "Email sending will fail until the Azure credentials are provided.");
		}
	}

	/** True once all four Azure credentials are present. */
	public boolean isConfigured() {
		return notBlank(tenantId) && notBlank(clientId)
				&& notBlank(clientSecret) && notBlank(sender);
	}

	/**
	 * Sends an HTML email. Throws {@link EmailSendException} if the message
	 * could not be handed to Microsoft Graph — the caller decides how to
	 * surface that (e.g. the OTP endpoint returns an error to the app).
	 */
	public void sendHtmlEmail(String toAddress, String subject, String htmlBody) {
		if (!isConfigured()) {
			throw new EmailSendException(
					"Email is not configured — set the graph.mail.* properties.");
		}

		Map<String, Object> payload = Map.of(
				"message", Map.of(
						"subject", subject,
						"body", Map.of("contentType", "HTML", "content", htmlBody),
						"toRecipients", List.of(
								Map.of("emailAddress", Map.of("address", toAddress)))),
				"saveToSentItems", false);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(accessToken());

		try {
			rest.postForEntity(
					String.format(SENDMAIL_URL, sender),
					new HttpEntity<>(payload, headers),
					String.class);
			log.info("Sent email to {} (subject: {})", toAddress, subject);
		} catch (Exception e) {
			log.error("Microsoft Graph sendMail failed for {}: {}", toAddress, e.getMessage());
			throw new EmailSendException("Failed to send email via Microsoft Graph.", e);
		}
	}

	/**
	 * Returns a valid app-only access token, fetching a fresh one from Azure AD
	 * when the cached token is missing or about to expire.
	 */
	private synchronized String accessToken() {
		if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
			return cachedToken;
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("scope", "https://graph.microsoft.com/.default");
		form.add("grant_type", "client_credentials");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> body = rest.postForObject(
					String.format(TOKEN_URL, tenantId),
					new HttpEntity<>(form, headers),
					Map.class);
			if (body == null || body.get("access_token") == null) {
				throw new EmailSendException("Azure token response contained no access_token.");
			}
			cachedToken = (String) body.get("access_token");
			long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
			// Refresh a minute early so a token never expires mid-request.
			tokenExpiry = Instant.now().plusSeconds(Math.max(expiresIn - 60, 30));
			return cachedToken;
		} catch (EmailSendException e) {
			throw e;
		} catch (Exception e) {
			log.error("Azure AD token request failed: {}", e.getMessage());
			throw new EmailSendException("Failed to obtain a Microsoft Graph access token.", e);
		}
	}

	private static boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}
}

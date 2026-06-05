package com.midco.rota;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.midco.rota.service.PasetoTokenService;

/**
 * Authenticates STOMP CONNECT frames by reading a PASETO from the
 * {@code Authorization: Bearer <token>} STOMP header and attaching the
 * resolved Principal to the session.
 *
 * <p>This is the canonical pattern for Spring Messaging WebSocket auth: the
 * WebSocket HTTP upgrade itself is left anonymous, and authentication is
 * performed at STOMP CONNECT time using a header that travels inside the
 * STOMP frame body rather than the URL query string. Tokens therefore never
 * appear in access logs, browser history, or referrer headers — the
 * shortcomings of the previous handshake-time {@code ?token=} approach.
 *
 * <p>Once the principal is set on the CONNECT frame, Spring's
 * SimpUserRegistry tracks the (username → sessionId) mapping for the
 * lifetime of the session. {@code convertAndSendToUser(...)} resolves
 * against that mapping, and subsequent SUBSCRIBE / SEND frames on the same
 * session inherit the principal automatically — only CONNECT needs the
 * header.
 *
 * <p>Failures (missing / malformed / invalid token) throw a
 * {@link MessagingException}, which Spring surfaces to the client as a
 * STOMP ERROR frame and closes the session. The default would be to
 * silently drop the message — we prefer a loud failure so the FE knows
 * its credentials were rejected.
 */
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

	private static final String AUTH_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final PasetoTokenService tokenService;

	public StompAuthChannelInterceptor(PasetoTokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			// Only the CONNECT frame carries credentials. Every other frame
			// (SUBSCRIBE, SEND, DISCONNECT, ...) inherits the principal that
			// was attached to the session on CONNECT, so we pass them
			// through untouched.
			return message;
		}

		String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);
		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			logger.warn("STOMP CONNECT rejected: missing or malformed Authorization header (sessionId={})",
					accessor.getSessionId());
			throw new MessagingException("Authentication required: missing Authorization header");
		}

		String token = authHeader.substring(BEARER_PREFIX.length()).trim();
		try {
			UsernamePasswordAuthenticationToken auth = tokenService.parseToken(token);
			accessor.setUser(auth);
			logger.debug("STOMP CONNECT authenticated: user='{}' sessionId={}", auth.getName(),
					accessor.getSessionId());
			return message;
		} catch (TokenValidationException ex) {
			// Surface the wrapped cause so we can distinguish "signature
			// mismatch" (stale token signed by an old key pair) from
			// "expired", "subject missing", etc. Without this we just see the
			// generic "Token parsing failed" wrapper.
			Throwable cause = ex.getCause();
			String causeSummary = cause != null
					? cause.getClass().getSimpleName() + ": " + cause.getMessage()
					: "(no cause)";
			logger.warn("STOMP CONNECT rejected: token validation failed (sessionId={}): {} | caused by {}",
					accessor.getSessionId(), ex.getMessage(), causeSummary);
			throw new MessagingException("Authentication failed: " + ex.getMessage());
		}
	}
}

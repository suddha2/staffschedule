package com.midco.rota;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.midco.rota.service.PasetoTokenService;

/**
 * STOMP-over-WebSocket configuration.
 *
 * <p>Authentication is performed at <b>STOMP CONNECT time</b> by
 * {@link StompAuthChannelInterceptor}, which reads a PASETO from the
 * {@code Authorization: Bearer ...} header on the CONNECT frame. The
 * HTTP/WebSocket upgrade itself is left anonymous: the connection is
 * cheap until the client successfully CONNECTs, and the broker won't
 * deliver any message to a session without a resolved principal.
 *
 * <p>This replaces the earlier {@code ?token=...} query-string approach
 * (TokenHandshakeHandler / AuthHandshakeInterceptor, now removed) which
 * leaked credentials into access logs.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final PasetoTokenService pasetoTokenService;

	public WebSocketConfig(PasetoTokenService pasetoTokenService) {
		this.pasetoTokenService = pasetoTokenService;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue"); // for broadcasting
		config.setApplicationDestinationPrefixes("/app"); // for client messages
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// No handshake-level auth: the upgrade is anonymous on purpose, the
		// real check is on the STOMP CONNECT frame below.
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		// Authenticate every STOMP CONNECT frame. The resolved principal is
		// attached to the session and reused by Spring's user-destination
		// resolver for the session's lifetime; subsequent SUBSCRIBE / SEND
		// frames inherit it without re-reading the token.
		registration.interceptors(new StompAuthChannelInterceptor(pasetoTokenService));
	}
}

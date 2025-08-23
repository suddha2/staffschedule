package com.midco.rota;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.midco.rota.service.PasetoTokenService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class TokenHandshakeHandler extends DefaultHandshakeHandler {

	private PasetoTokenService pasetoTokenService;

	public TokenHandshakeHandler(PasetoTokenService pasetoTokenService) {
		this.pasetoTokenService = pasetoTokenService;
	}

	@Override
	protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		String token = (String) attributes.get("token");

		if (token == null || token.isBlank()) {
			return null; // no principal bound
		}

		// ✅ Extract username from PASETO token
		Optional<String> username = pasetoTokenService.validateToken(token); // implement this

		return username.map(name -> new UsernamePasswordAuthenticationToken(name, null)).orElse(null);
	}
}

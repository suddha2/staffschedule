package com.midco.rota;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.midco.rota.service.PasetoTokenService;

public class TokenHandshakeHandler extends DefaultHandshakeHandler {

	private PasetoTokenService pasetoTokenService;

	public TokenHandshakeHandler(PasetoTokenService pasetoTokenService) {
		this.pasetoTokenService = pasetoTokenService;
	}

	@Override
	protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
	                                  Map<String, Object> attributes) {
	    String token = (String) attributes.get("token");
	    if (token == null || token.isBlank()) return null;

	    try {
	        UsernamePasswordAuthenticationToken auth = pasetoTokenService.parseToken(token);
	        return auth; // UsernamePasswordAuthenticationToken implements Principal
	    } catch (TokenValidationException ex) {
	        return null;
	    }
	}
}

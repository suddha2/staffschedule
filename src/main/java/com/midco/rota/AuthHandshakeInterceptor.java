package com.midco.rota;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class AuthHandshakeInterceptor implements HandshakeInterceptor {

	 @Override
	    public boolean beforeHandshake(
	        ServerHttpRequest request,
	        ServerHttpResponse response,
	        WebSocketHandler wsHandler,
	        Map<String, Object> attributes
	    ) {
	        if (request instanceof ServletServerHttpRequest servletRequest) {
	            String token = servletRequest.getServletRequest().getParameter("token");
	            // Spring's attributes map is a ConcurrentHashMap, which rejects nulls.
	            // SockJS follow-up handshake requests don't always carry the ?token= query param;
	            // skip the put when missing so TokenHandshakeHandler can fall back to anonymous.
	            if (token != null && !token.isBlank()) {
	                attributes.put("token", token);
	            }
	        }
	        return true;
	    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}


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
	            attributes.put("token", token);
	        }
	        return true;
	    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}


package com.midco.rota;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.midco.rota.service.PasetoTokenService;



@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final PasetoTokenService pasetoTokenService;
	
	public WebSocketConfig(PasetoTokenService pasetoTokenService) {
        this.pasetoTokenService = pasetoTokenService;
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/queue"); // for broadcasting
        config.setApplicationDestinationPrefixes("/app"); // for client messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
                .addInterceptors(new AuthHandshakeInterceptor()) // 👈 add this
                .setHandshakeHandler(new TokenHandshakeHandler(pasetoTokenService))
                .withSockJS();
    }
}

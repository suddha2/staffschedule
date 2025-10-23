package com.midco.rota;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.task.DelegatingSecurityContextTaskExecutor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Autowired
	private final PasetoAuthenticationFilter pasetoAuthenticationFilter;

	public SecurityConfig(PasetoAuthenticationFilter pasetoAuthenticationFilter) {

		this.pasetoAuthenticationFilter = pasetoAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.securityContext(securityContext -> securityContext.requireExplicitSave(false)) // Required to ensure SecurityContext is saved and restored across async dispatchers 
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll()

						// Allow OPTIONS requests for CORS
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// Public endpoints
						.requestMatchers("/login").permitAll().requestMatchers("/ws/**").permitAll()
//						.requestMatchers("/api/stats/**").permitAll()
						.anyRequest().authenticated())
				// Add Paseto filter before UsernamePasswordAuthenticationFilter
				.addFilterBefore(pasetoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean(name = "applicationTaskExecutor")
	public AsyncTaskExecutor applicationTaskExecutor() {
		ThreadPoolTaskExecutor baseExecutor = new ThreadPoolTaskExecutor();
		baseExecutor.setCorePoolSize(10);
		baseExecutor.setMaxPoolSize(20);
		baseExecutor.setQueueCapacity(100);
		baseExecutor.setThreadNamePrefix("secure-async-");
		baseExecutor.initialize();
		DelegatingSecurityContextTaskExecutor securityExecutor = new DelegatingSecurityContextTaskExecutor(
				baseExecutor);
		System.out.println("Secure async executor initialized");
		// Bridge to AsyncTaskExecutor
		return new ConcurrentTaskExecutor(securityExecutor);

	}
}
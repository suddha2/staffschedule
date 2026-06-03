package com.midco.rota;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
						.requestMatchers("/api/auth/login").permitAll()
						.requestMatchers("/api/auth/microsoft").permitAll()
						.requestMatchers("/ws/**").permitAll()
						.requestMatchers("/api/mobile/auth/**").permitAll()
						// Mobile employee endpoints
						.requestMatchers("/api/mobile/**").hasRole("EMPLOYEE")
						// Publishing unallocated shifts (POST) is open to schedulers, ops
						// managers and admins; the publish history/log GETs fall through
						// to .anyRequest().authenticated() so any signed-in user (incl.
						// READ_ONLY) can view them.
						.requestMatchers(HttpMethod.POST, "/api/stats/publish/**")
								.hasAnyRole("ADMIN", "OPS_MANAGER", "ROTA_EDITOR")
						// User registration is admin-only (case fixed: was 'admin').
						.requestMatchers("/api/auth/register").hasRole("ADMIN")
						// Resolving shift requests is open to schedulers, ops managers and admins.
						// Listed BEFORE the broader /api/admin/** rule so the more specific
						// path-match wins.
						.requestMatchers("/api/admin/shift-requests/**")
								.hasAnyRole("ADMIN", "OPS_MANAGER", "ROTA_EDITOR")
						// User management is also open to ops managers; the controller
						// enforces that non-admins can't touch ADMIN users or grant the
						// ADMIN role (privilege-escalation guard).
						.requestMatchers("/api/admin/users/**")
								.hasAnyRole("ADMIN", "OPS_MANAGER")
						// Everything else under /api/admin/ remains admin-only.
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						// All other endpoints just require a valid token; finer-grained role
						// checks for write operations live as @PreAuthorize on the methods.
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
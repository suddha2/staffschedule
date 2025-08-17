package com.midco.rota;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.midco.rota.service.PasetoTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

	private final PasetoTokenService tokenService;

	public PasetoAuthenticationFilter(PasetoTokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = request.getHeader("Authorization");
		if (token != null && token.startsWith("Bearer ")) {
			tokenService.validateToken(token.substring(7)).ifPresent(username -> {
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
						List.of());
				SecurityContextHolder.getContext().setAuthentication(auth);
			});
		}
		chain.doFilter(request, response);
	}
}

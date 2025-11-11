package com.midco.rota;

import java.io.IOException;

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

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			try {
				UsernamePasswordAuthenticationToken auth = tokenService.parseToken(header.substring(7));

				SecurityContextHolder.getContext().setAuthentication(auth);

			} catch (TokenValidationException ex) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json");
				response.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
				return;
			}
		}

		chain.doFilter(request, response);
	}
}

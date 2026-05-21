package com.midco.rota;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.midco.rota.model.Employee;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.PasetoTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

	private final PasetoTokenService tokenService;
	private final EmployeeRepository employeeRepository;

	public PasetoAuthenticationFilter(PasetoTokenService tokenService,
			EmployeeRepository employeeRepository) {
		this.tokenService = tokenService;
		this.employeeRepository = employeeRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			try {
				UsernamePasswordAuthenticationToken auth = tokenService.parseToken(header.substring(7));

				// Employee (mobile) tokens carry ROLE_EMPLOYEE with the employee's
				// email as the subject. Reject the moment that employee is set
				// inactive, so a leaver's still-valid 30-day token stops working
				// immediately instead of lingering until it expires. Admin tokens
				// (no ROLE_EMPLOYEE) skip this lookup.
				boolean isEmployee = auth.getAuthorities().stream()
						.anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()));
				if (isEmployee) {
					boolean active = employeeRepository.findByEmail(auth.getName())
							.map(Employee::isActive)
							.orElse(false);
					if (!active) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("application/json");
						response.getWriter().write("{\"error\":\"Account is inactive\"}");
						return;
					}
				}

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

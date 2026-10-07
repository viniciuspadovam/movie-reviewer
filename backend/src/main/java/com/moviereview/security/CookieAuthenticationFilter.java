package com.moviereview.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Deliberately not Spring's oauth2ResourceServer: it exempts every request carrying a "bearer" token from CSRF
// checks, and here the token is a cookie the browser attaches by itself, which would switch CSRF off.
class CookieAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(CookieAuthenticationFilter.class);

	private final JwtDecoder jwtDecoder;

	private final String protectedPrefix;

	private final String sessionPath;

	CookieAuthenticationFilter(JwtDecoder jwtDecoder, String protectedPrefix, String sessionPath) {
		this.jwtDecoder = jwtDecoder;
		this.protectedPrefix = protectedPrefix;
		this.sessionPath = sessionPath;
	}

	// Only protected routes read the cookie: a stale token must not turn public pages into 401s.
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		String token = path.startsWith(protectedPrefix) || path.equals(sessionPath) ? AuthCookies.readToken(request)
				: null;
		if (token != null) {
			authenticate(token);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(String token) {
		try {
			Jwt jwt = jwtDecoder.decode(token);
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
			SecurityContextHolder.setContext(context);
		}
		catch (JwtException exception) {
			log.debug("Token de sessão rejeitado: {}", exception.getMessage());
		}
	}

}

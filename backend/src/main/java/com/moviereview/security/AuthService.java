package com.moviereview.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final AuthProperties properties;

	private final PasswordEncoder passwordEncoder;

	private final LoginRateLimiter rateLimiter;

	private final TokenService tokenService;

	AuthService(AuthProperties properties, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter,
			TokenService tokenService) {
		this.properties = properties;
		this.passwordEncoder = passwordEncoder;
		this.rateLimiter = rateLimiter;
		this.tokenService = tokenService;
	}

	String login(String username, String password, String clientIp) {
		rateLimiter.consume(clientIp);
		// Always run the hash check so a wrong username takes as long as a wrong password.
		boolean passwordMatches = passwordEncoder.matches(password, properties.passwordHash());
		boolean usernameMatches = MessageDigest.isEqual(username.getBytes(StandardCharsets.UTF_8),
				properties.username().getBytes(StandardCharsets.UTF_8));
		if (!(passwordMatches && usernameMatches)) {
			log.warn("Tentativa de login inválida a partir do IP {}", clientIp);
			throw new InvalidCredentialsException();
		}
		log.info("Login realizado a partir do IP {}", clientIp);
		return tokenService.issue(properties.username());
	}

}

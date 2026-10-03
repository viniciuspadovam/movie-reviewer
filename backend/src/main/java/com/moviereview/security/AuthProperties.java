package com.moviereview.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
		@NotBlank String username,
		@NotBlank String passwordHash,
		@NotBlank @Size(min = 32) String jwtSecret,
		@NotNull Duration sessionTtl,
		String cookieDomain,
		boolean cookieSecure,
		@NotNull LoginRateLimit loginRateLimit) {

	public record LoginRateLimit(int attempts, @NotNull Duration period) {
	}

	public boolean hasCookieDomain() {
		return cookieDomain != null && !cookieDomain.isBlank();
	}

}

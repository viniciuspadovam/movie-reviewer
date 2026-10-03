package com.moviereview.tmdb;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("app.tmdb")
public record TmdbProperties(
		@NotBlank String baseUrl,
		String accessToken,
		@NotBlank String language,
		@NotNull Duration timeout) {

	boolean isConfigured() {
		return accessToken != null && !accessToken.isBlank();
	}

}

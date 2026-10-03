package com.moviereview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.web")
public record WebProperties(@NotBlank String frontendOrigin) {
}

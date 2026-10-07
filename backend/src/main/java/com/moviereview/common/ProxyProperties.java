package com.moviereview.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.proxy")
public record ProxyProperties(String sharedSecret) {

	boolean isConfigured() {
		return sharedSecret != null && !sharedSecret.isBlank();
	}

}

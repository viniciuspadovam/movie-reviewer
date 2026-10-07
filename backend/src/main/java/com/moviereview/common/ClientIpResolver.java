package com.moviereview.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

	private static final String CLIENT_IP_HEADER = "X-Client-IP";

	private static final String PROXY_SECRET_HEADER = "X-Proxy-Secret";

	private final ProxyProperties properties;

	public ClientIpResolver(ProxyProperties properties) {
		this.properties = properties;
	}

	// The Cloud Run URL is public, so the client IP sent by the Cloudflare Worker is only believed when it
	// comes with the shared secret; otherwise anyone could fake an IP per request and dodge the login limit.
	public String resolve(HttpServletRequest request) {
		String forwardedIp = request.getHeader(CLIENT_IP_HEADER);
		if (isFromTrustedProxy(request) && forwardedIp != null && !forwardedIp.isBlank()) {
			return forwardedIp.strip();
		}
		return request.getRemoteAddr();
	}

	private boolean isFromTrustedProxy(HttpServletRequest request) {
		String provided = request.getHeader(PROXY_SECRET_HEADER);
		if (!properties.isConfigured() || provided == null) {
			return false;
		}
		return MessageDigest.isEqual(properties.sharedSecret().getBytes(StandardCharsets.UTF_8),
				provided.getBytes(StandardCharsets.UTF_8));
	}

}

package com.moviereview.common;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

	// The API is reachable only through the Cloudflare Tunnel, so this header is set by Cloudflare and cannot be spoofed.
	private static final String CLOUDFLARE_IP_HEADER = "CF-Connecting-IP";

	public String resolve(HttpServletRequest request) {
		String cloudflareIp = request.getHeader(CLOUDFLARE_IP_HEADER);
		return cloudflareIp == null || cloudflareIp.isBlank() ? request.getRemoteAddr() : cloudflareIp.strip();
	}

}

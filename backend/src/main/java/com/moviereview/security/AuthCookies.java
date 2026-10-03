package com.moviereview.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
class AuthCookies {

	static final String AUTH_COOKIE = "AUTH_TOKEN";

	private final AuthProperties properties;

	AuthCookies(AuthProperties properties) {
		this.properties = properties;
	}

	ResponseCookie session(String token) {
		return base(token).maxAge(properties.sessionTtl()).build();
	}

	ResponseCookie expired() {
		return base("").maxAge(Duration.ZERO).build();
	}

	static String readToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (AUTH_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(AUTH_COOKIE, value)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite("Strict")
			.path("/");
		if (properties.hasCookieDomain()) {
			builder.domain(properties.cookieDomain());
		}
		return builder;
	}

}

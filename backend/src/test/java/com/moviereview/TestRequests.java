package com.moviereview;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.Cookie;

public final class TestRequests {

	private TestRequests() {
	}

	// Sends the double-submit pair like the SPA does. Spring Security's csrf() post-processor is avoided on purpose:
	// it swaps the filter's repository in the cached context and silences the real XSRF-TOKEN cookie for later tests.
	public static RequestPostProcessor xsrf() {
		String token = UUID.randomUUID().toString();
		return request -> {
			request.setCookies(appendCookie(request.getCookies(), new Cookie("XSRF-TOKEN", token)));
			request.addHeader("X-XSRF-TOKEN", token);
			return request;
		};
	}

	public static RequestPostProcessor fromNewIp() {
		String ip = UUID.randomUUID().toString();
		return request -> {
			request.addHeader("CF-Connecting-IP", ip);
			return request;
		};
	}

	public static Cookie adminSession(MockMvc mockMvc) throws Exception {
		return mockMvc
			.perform(post("/api/v1/auth/login").with(xsrf())
				.with(fromNewIp())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "admin", "password": "admin"}"""))
			.andReturn()
			.getResponse()
			.getCookie("AUTH_TOKEN");
	}

	private static Cookie[] appendCookie(Cookie[] cookies, Cookie cookie) {
		if (cookies == null) {
			return new Cookie[] { cookie };
		}
		Cookie[] result = Arrays.copyOf(cookies, cookies.length + 1);
		result[cookies.length] = cookie;
		return result;
	}

}

package com.moviereview.security;

import static com.moviereview.TestRequests.fromNewIp;
import static com.moviereview.TestRequests.xsrf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.moviereview.IntegrationTest;

import jakarta.servlet.http.Cookie;

@IntegrationTest
class SecurityIntegrationTest {

	private static final String VALID_LOGIN = """
			{"username": "admin", "password": "admin"}""";

	private static final String WRONG_LOGIN = """
			{"username": "admin", "password": "wrong"}""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void adminRoutesRejectAnonymousRequests() throws Exception {
		mockMvc.perform(get("/api/v1/admin/reviews")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/admin/titles").with(xsrf())).andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithoutCsrfTokenIsForbidden() throws Exception {
		mockMvc.perform(login(VALID_LOGIN).with(fromNewIp())).andExpect(status().isForbidden());
	}

	@Test
	void loginSetsSecureSessionCookieThatAuthenticates() throws Exception {
		Cookie session = mockMvc.perform(login(VALID_LOGIN).with(xsrf()).with(fromNewIp()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(
					allOf(containsString("AUTH_TOKEN="), containsString("HttpOnly"), containsString("SameSite=Strict")))))
			.andReturn()
			.getResponse()
			.getCookie(AuthCookies.AUTH_COOKIE);

		mockMvc.perform(get("/api/v1/auth/me").cookie(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"));
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		mockMvc.perform(login(WRONG_LOGIN).with(xsrf()).with(fromNewIp())).andExpect(status().isUnauthorized());
	}

	@Test
	void sixthAttemptWithinWindowIsRateLimited() throws Exception {
		String ip = UUID.randomUUID().toString();
		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(login(WRONG_LOGIN).with(xsrf()).header("CF-Connecting-IP", ip))
				.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(login(WRONG_LOGIN).with(xsrf()).header("CF-Connecting-IP", ip))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER));
	}

	@Test
	void invalidCookieDoesNotBreakPublicRoutes() throws Exception {
		mockMvc.perform(get("/api/v1/does-not-exist").cookie(new Cookie(AuthCookies.AUTH_COOKIE, "garbage")))
			.andExpect(status().isNotFound());
	}

	@Test
	void logoutExpiresSessionCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout").with(xsrf()))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge(AuthCookies.AUTH_COOKIE, 0));
	}

	@Test
	void anyResponseCarriesXsrfCookieForTheSpa() throws Exception {
		mockMvc.perform(get("/api/v1/does-not-exist"))
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andExpect(cookie().httpOnly("XSRF-TOKEN", false))
			.andExpect(cookie().sameSite("XSRF-TOKEN", "Strict"));
	}

	private static MockHttpServletRequestBuilder login(String body) {
		return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
	}

}

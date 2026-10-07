package com.moviereview.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

	private static final String SECRET = "shared-secret";

	private final ClientIpResolver resolver = new ClientIpResolver(new ProxyProperties(SECRET));

	@Test
	void usesTheForwardedIpWhenTheSecretMatches() {
		MockHttpServletRequest request = request("203.0.113.9", SECRET);

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
	}

	@Test
	void ignoresTheForwardedIpWhenTheSecretIsWrongOrMissing() {
		assertThat(resolver.resolve(request("203.0.113.9", "wrong"))).isEqualTo("10.0.0.1");
		assertThat(resolver.resolve(request("203.0.113.9", null))).isEqualTo("10.0.0.1");
	}

	@Test
	void neverTrustsTheHeaderWhenNoSecretIsConfigured() {
		ClientIpResolver unconfigured = new ClientIpResolver(new ProxyProperties(""));

		assertThat(unconfigured.resolve(request("203.0.113.9", ""))).isEqualTo("10.0.0.1");
	}

	@Test
	void fallsBackToTheRemoteAddressWithoutAForwardedIp() {
		assertThat(resolver.resolve(request(null, SECRET))).isEqualTo("10.0.0.1");
	}

	private static MockHttpServletRequest request(String clientIp, String secret) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.0.0.1");
		if (clientIp != null) {
			request.addHeader("X-Client-IP", clientIp);
		}
		if (secret != null) {
			request.addHeader("X-Proxy-Secret", secret);
		}
		return request;
	}

}

package com.moviereview.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.moviereview.config.WebProperties;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	private static final String API = "/api/v1";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties authProperties) throws Exception {
		http
			.csrf(csrf -> csrf
				.csrfTokenRepository(csrfTokenRepository(authProperties))
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.cors(withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health/**", "/error").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/logout").permitAll()
				.requestMatchers(API + "/auth/me", API + "/admin/**").authenticated()
				.requestMatchers(HttpMethod.GET, API + "/**").permitAll()
				.anyRequest().denyAll())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.bearerTokenResolver(cookieTokenResolver())
				.jwt(withDefaults()));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(WebProperties webProperties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(webProperties.frontendOrigin()));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration(API + "/**", config);
		return source;
	}

	// Only protected routes read the cookie: a stale token must not turn public pages into 401s.
	private static BearerTokenResolver cookieTokenResolver() {
		return request -> {
			String path = request.getRequestURI().substring(request.getContextPath().length());
			boolean protectedPath = path.startsWith(API + "/admin/") || path.equals(API + "/auth/me");
			return protectedPath ? AuthCookies.readToken(request) : null;
		};
	}

	private static CookieCsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> {
			cookie.secure(properties.cookieSecure()).sameSite("Strict").path("/");
			if (properties.hasCookieDomain()) {
				cookie.domain(properties.cookieDomain());
			}
		});
		return repository;
	}

}

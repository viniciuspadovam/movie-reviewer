package com.moviereview.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviereview.common.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AuthService authService;

	private final AuthCookies authCookies;

	private final ClientIpResolver clientIpResolver;

	AuthController(AuthService authService, AuthCookies authCookies, ClientIpResolver clientIpResolver) {
		this.authService = authService;
		this.authCookies = authCookies;
		this.clientIpResolver = clientIpResolver;
	}

	@PostMapping("/login")
	ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest login, HttpServletRequest request) {
		String token = authService.login(login.username(), login.password(), clientIpResolver.resolve(request));
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, authCookies.session(token).toString())
			.body(new UserResponse(login.username()));
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, authCookies.expired().toString()).build();
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new UserResponse(jwt.getSubject());
	}

	record LoginRequest(@NotBlank(message = "Informe o usuário.") String username,
			@NotBlank(message = "Informe a senha.") String password) {
	}

	record UserResponse(String username) {
	}

}

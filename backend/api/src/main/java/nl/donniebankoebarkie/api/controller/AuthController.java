package nl.donniebankoebarkie.api.controller;

import jakarta.validation.Valid;
import nl.donniebankoebarkie.api.config.ApplicationEnvironment;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.auth.request.LoginRequest;
import nl.donniebankoebarkie.api.dto.auth.request.RegisterRequest;
import nl.donniebankoebarkie.api.dto.auth.response.AuthResponse;
import nl.donniebankoebarkie.api.dto.auth.response.RefreshResponse;
import nl.donniebankoebarkie.api.service.interfaces.IAuthService;
import nl.donniebankoebarkie.api.service.result.LoginResult;
import nl.donniebankoebarkie.api.service.result.RefreshResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final IAuthService authService;
    private final boolean secureRefreshCookie;

    public AuthController(IAuthService authService, ApplicationEnvironment applicationEnvironment) {
        this.authService = authService;
        this.secureRefreshCookie = applicationEnvironment.isProduction();
    }

    // Registers a new customer and returns the created user.
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // Authenticates a user and stores the refresh token in an HttpOnly cookie.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return okWithRefreshCookie(result.response(), result.refreshToken(), result.refreshTokenExpiresIn());
    }

    // Rotates the refresh token from the cookie and returns a new access token.
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        RefreshResult result = authService.refresh(refreshToken);
        return okWithRefreshCookie(result.response(), result.refreshToken(), result.refreshTokenExpiresIn());
    }

    // Revokes the current refresh token and clears the browser cookie.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    // Adds the refresh cookie consistently to successful auth responses.
    private <T> ResponseEntity<T> okWithRefreshCookie(T body, String refreshToken, long maxAgeSeconds) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, maxAgeSeconds).toString())
                .body(body);
    }

    // Builds the refresh cookie; Secure is only enabled in production for local development support.
    private ResponseCookie buildRefreshCookie(String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureRefreshCookie)
                .path("/")
                .sameSite("Strict")
                .maxAge(maxAgeSeconds)
                .build();
    }

    // Expires the refresh cookie on the client after logout.
    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureRefreshCookie)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

}

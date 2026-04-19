package nl.donniebankoebarkie.api.controller;

import nl.donniebankoebarkie.api.config.ApplicationEnvironment;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.auth.request.LoginRequest;
import nl.donniebankoebarkie.api.dto.auth.request.RegisterRequest;
import nl.donniebankoebarkie.api.dto.auth.response.AuthResponse;
import nl.donniebankoebarkie.api.dto.auth.response.RefreshResponse;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.service.interfaces.IAuthService;
import nl.donniebankoebarkie.api.service.result.LoginResult;
import nl.donniebankoebarkie.api.service.result.RefreshResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IAuthService authService;

    @Mock
    private ApplicationEnvironment applicationEnvironment;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerReturnsCreatedUserAndLocationHeader() {
        RegisterRequest request = new RegisterRequest(
                "Charlie",
                "Student",
                "charlie.student@example.com",
                "welkom123",
                "+31611112222",
                "111222333"
        );
        UserResponse response = new UserResponse(
                3L,
                "Charlie",
                "Student",
                "charlie.student@example.com",
                "+31611112222",
                "111222333",
                UserRole.CUSTOMER,
                false,
                null,
                null,
                LocalDateTime.of(2026, 4, 19, 10, 0),
                LocalDateTime.of(2026, 4, 19, 10, 0)
        );
        when(authService.register(request)).thenReturn(response);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/auth/register");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        try {
            var entity = authController.register(request);

            assertEquals(201, entity.getStatusCode().value());
            assertEquals("Charlie", entity.getBody().firstName());
            assertTrue(entity.getHeaders().getLocation() != null);
            assertTrue(entity.getHeaders().getLocation().toString().contains("/api/users/3"));
        }
        finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void loginReturnsTokensAndRefreshCookie() {
        LoginRequest request = new LoginRequest("emma.customer@projectbank.local", "welkom123");
        when(authService.login(request)).thenReturn(new LoginResult(
                new AuthResponse(
                        "access-token",
                        "Bearer",
                        900,
                        new UserResponse(
                                1L,
                                "Emma",
                                "Customer",
                                "emma.customer@projectbank.local",
                                "+31612345678",
                                "123456789",
                                UserRole.CUSTOMER,
                                true,
                                LocalDateTime.of(2026, 4, 19, 9, 5),
                                2L,
                                LocalDateTime.of(2026, 4, 19, 9, 0),
                                LocalDateTime.of(2026, 4, 19, 9, 5)
                        )
                ),
                "refresh-token",
                604800
        ));

        var entity = authController.login(request);

        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Bearer", entity.getBody().tokenType());
        assertTrue(entity.getHeaders().getFirst("Set-Cookie").contains("refresh_token=refresh-token"));
    }

    @Test
    void refreshReturnsNewAccessTokenAndRotatedCookie() {
        when(authService.refresh("refresh-token")).thenReturn(new RefreshResult(
                new RefreshResponse("new-access-token", "Bearer", 900),
                "rotated-refresh-token",
                604800
        ));

        var entity = authController.refresh("refresh-token");

        assertEquals(200, entity.getStatusCode().value());
        assertEquals("new-access-token", entity.getBody().accessToken());
        assertTrue(entity.getHeaders().getFirst("Set-Cookie").contains("refresh_token=rotated-refresh-token"));
    }

    @Test
    void logoutClearsTheRefreshCookie() {
        var entity = authController.logout("refresh-token");

        assertEquals(204, entity.getStatusCode().value());
        assertTrue(entity.getHeaders().getFirst("Set-Cookie").contains("Max-Age=0"));
    }
}

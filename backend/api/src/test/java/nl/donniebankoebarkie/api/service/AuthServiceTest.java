package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.config.JwtProperties;
import nl.donniebankoebarkie.api.dto.auth.request.LoginRequest;
import nl.donniebankoebarkie.api.dto.auth.request.RegisterRequest;
import nl.donniebankoebarkie.api.exception.AuthenticationFailedException;
import nl.donniebankoebarkie.api.exception.ConflictException;
import nl.donniebankoebarkie.api.model.RefreshToken;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IRefreshTokenRepository;
import nl.donniebankoebarkie.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String STRONG_PASSWORD = "Welkom123";


    @Mock
    private IAuthRepository authRepository;

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordAndSavesUnapprovedCustomer() {
        RegisterRequest request = new RegisterRequest(
                "Charlie",
                "Student",
                "charlie.student@example.com",
                STRONG_PASSWORD,
                "+31611112222",
                "111222333");
        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByBsnNumber(request.bsnNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        var response = authService.register(request);

        assertEquals(3L, response.id());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertFalse(response.approved());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Charlie",
                "Student",
                "charlie.student@example.com",
                STRONG_PASSWORD,
                "+31611112222",
                "111222333");
        when(authRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        verify(authRepository, never()).save(any(User.class));
        verifyNoInteractions(refreshTokenRepository, jwtService, passwordEncoder);
    }

    @Test
    void registerRejectsDuplicateBsnNumber() {
        RegisterRequest request = new RegisterRequest(
                "Charlie",
                "Student",
                "charlie.student@example.com",
                STRONG_PASSWORD,
                "+31611112222",
                "111222333");
        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByBsnNumber(request.bsnNumber())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        verify(authRepository, never()).save(any(User.class));
        verifyNoInteractions(refreshTokenRepository, jwtService, passwordEncoder);
    }

    @Test
    void registerCreatesOnlyCustomerUserWithoutAuthArtifacts() {
        RegisterRequest request = new RegisterRequest(
                "Charlie",
                "Student",
                "charlie.student@example.com",
                STRONG_PASSWORD,
                "+31611112222",
                "111222333");
        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByBsnNumber(request.bsnNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        authService.register(request);

        verify(authRepository).save(any(User.class));
        verifyNoInteractions(refreshTokenRepository, jwtService);
    }

    @Test
    void loginReturnsAccessTokenAndPersistsHashedRefreshToken() {
        User user = approvedCustomer();
        when(authRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("welkom123", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProperties.accessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtProperties.refreshTokenExpirationSeconds()).thenReturn(604800L);

        var result = authService.login(new LoginRequest(user.getEmail(), "welkom123"));

        assertEquals("access-token", result.response().accessToken());
        assertEquals("Bearer", result.response().tokenType());
        assertNotNull(result.refreshToken());
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals(user.getId(), tokenCaptor.getValue().getUserId());
        assertNotEquals(result.refreshToken(), tokenCaptor.getValue().getTokenHash());
    }

    @Test
    void refreshRotatesRefreshTokenAndReturnsNewAccessToken() {
        User user = approvedCustomer();
        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(9L);
        storedToken.setUserId(user.getId());
        storedToken.setTokenHash("hashed-old-token");
        storedToken.setExpiresAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        storedToken.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(authRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtProperties.accessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtProperties.refreshTokenExpirationSeconds()).thenReturn(604800L);

        var result = authService.refresh("old-refresh-token");

        assertEquals("new-access-token", result.response().accessToken());
        assertNotNull(result.refreshToken());
        verify(refreshTokenRepository).revoke(storedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void logoutRevokesPresentedRefreshTokenWhenItExists() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(11L);
        storedToken.setUserId(1L);
        storedToken.setTokenHash("hashed-refresh-token");
        storedToken.setExpiresAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        storedToken.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        verify(refreshTokenRepository).revoke(storedToken);
    }

    @Test
    void loginRejectsInvalidCredentials() {
        User user = approvedCustomer();
        when(authRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("welkom123", user.getPasswordHash())).thenReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest(user.getEmail(), "welkom123")));
    }

    private User approvedCustomer() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Emma");
        user.setLastName("Customer");
        user.setEmail("emma.customer@projectbank.local");
        user.setPasswordHash("encoded-password");
        user.setPhoneNumber("+31612345678");
        user.setBsnNumber("123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setApproved(true);
        user.setApprovedByUserId(2L);
        user.setApprovedAt(LocalDateTime.of(2026, 4, 19, 9, 5));
        user.setCreatedAt(LocalDateTime.of(2026, 4, 19, 9, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 4, 19, 9, 5));
        return user;
    }
}

package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.config.JwtProperties;
import nl.donniebankoebarkie.api.dto.auth.request.LoginRequest;
import nl.donniebankoebarkie.api.dto.auth.request.RegisterRequest;
import nl.donniebankoebarkie.api.dto.auth.response.AuthResponse;
import nl.donniebankoebarkie.api.dto.auth.response.RefreshResponse;
import nl.donniebankoebarkie.api.exception.AuthenticationFailedException;
import nl.donniebankoebarkie.api.exception.ConflictException;
import nl.donniebankoebarkie.api.exception.InvalidRefreshTokenException;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.mapper.UserMapper;
import nl.donniebankoebarkie.api.model.RefreshToken;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IRefreshTokenRepository;
import nl.donniebankoebarkie.api.security.JwtService;
import nl.donniebankoebarkie.api.service.interfaces.IAuthService;
import nl.donniebankoebarkie.api.service.result.LoginResult;
import nl.donniebankoebarkie.api.service.result.RefreshResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthService implements IAuthService {
    private final IAuthRepository authRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            IAuthRepository authRepository,
            IRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.authRepository = authRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    // Creates a new unapproved customer account after checking unique identity fields.
    @Override
    public UserResponse register(RegisterRequest request) {
        validateUniqueRegistration(request);
        User user = createCustomerUser(request);
        return UserMapper.toUserResponse(authRepository.save(user));
    }

    // Validates credentials and issues access and refresh tokens.
    @Override
    public LoginResult login(LoginRequest request) {
        User user = getUserForLogin(request.email());
        validatePassword(request.password(), user);
        return createLoginResult(user);
    }

    // Validates the refresh token and rotates it to limit token replay.
    @Override
    public RefreshResult refresh(String refreshToken) {
        String sanitizedRefreshToken = requireRefreshToken(refreshToken);
        RefreshToken storedToken = getUsableRefreshToken(sanitizedRefreshToken);
        User user = getRefreshTokenUser(storedToken);

        return rotateRefreshToken(storedToken, user);
    }

    // Revokes the refresh token when present; missing tokens make logout idempotent.
    @Override
    public void logout(String refreshToken) {
        String sanitizedRefreshToken = sanitizeRefreshToken(refreshToken);

        if (sanitizedRefreshToken == null || sanitizedRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(sanitizedRefreshToken))
                .ifPresent(refreshTokenRepository::revoke);
    }

    // Returns the authenticated user's public profile data.
    @Override
    public UserResponse getCurrentUser(Long userId) {
        return authRepository.findById(userId)
                .map(UserMapper::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found."));
    }

    // Prevents duplicate registrations for login and BSN identity fields.
    private void validateUniqueRegistration(RegisterRequest request) {
        if (authRepository.existsByEmail(request.email())) {
            throw new ConflictException("A user with that email already exists.");
        }
        if (authRepository.existsByBsnNumber(request.bsnNumber())) {
            throw new ConflictException("A user with that BSN already exists.");
        }
    }

    // Builds a customer user that starts unapproved until an employee approves it.
    private User createCustomerUser(RegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return new User(
                null,
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.phoneNumber(),
                request.bsnNumber(),
                UserRole.CUSTOMER,
                false,
                null,
                null,
                now,
                now
        );
    }

    // Loads the user for authentication and hides whether the email exists.
    private User getUserForLogin(String email) {
        return authRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password."));
    }

    // Checks the submitted password against the stored BCrypt hash.
    private void validatePassword(String password, User user) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password.");
        }
    }

    // Creates the response DTO and persists the refresh token hash.
    private LoginResult createLoginResult(User user) {
        String rawRefreshToken = generateRefreshToken();
        saveRefreshToken(user.getId(), rawRefreshToken);

        return new LoginResult(
                new AuthResponse(
                        jwtService.generateAccessToken(user),
                        "Bearer",
                        jwtProperties.accessTokenExpirationSeconds(),
                        UserMapper.toUserResponse(user)
                ),
                rawRefreshToken,
                jwtProperties.refreshTokenExpirationSeconds()
        );
    }

    // Requires a non-blank refresh token before repository lookup.
    private String requireRefreshToken(String refreshToken) {
        String sanitizedRefreshToken = sanitizeRefreshToken(refreshToken);

        if (sanitizedRefreshToken == null || sanitizedRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing or invalid.");
        }

        return sanitizedRefreshToken;
    }

    // Trims cookie input while preserving null for optional logout handling.
    private String sanitizeRefreshToken(String refreshToken) {
        return refreshToken == null ? null : refreshToken.trim();
    }

    // Loads a refresh token that is known, active, and not expired.
    private RefreshToken getUsableRefreshToken(String rawRefreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is missing or invalid."));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked.");
        }

        return storedToken;
    }

    // Loads the token owner and fails refresh when the user no longer exists.
    private User getRefreshTokenUser(RefreshToken refreshToken) {
        return authRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token user was not found."));
    }

    // Revokes the used refresh token and stores a replacement in the same transaction.
    private RefreshResult rotateRefreshToken(RefreshToken storedToken, User user) {
        refreshTokenRepository.revoke(storedToken);
        String rotatedRefreshToken = generateRefreshToken();
        saveRefreshToken(user.getId(), rotatedRefreshToken);

        return new RefreshResult(
                new RefreshResponse(
                        jwtService.generateAccessToken(user),
                        "Bearer",
                        jwtProperties.accessTokenExpirationSeconds()
                ),
                rotatedRefreshToken,
                jwtProperties.refreshTokenExpirationSeconds()
        );
    }

    // Stores only a hash of the refresh token so raw tokens are never persisted.
    private void saveRefreshToken(Long userId, String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hashToken(rawRefreshToken));
        token.setExpiresAt(now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds()));
        token.setRevoked(false);
        token.setCreatedAt(now);
        token.setUpdatedAt(now);
        refreshTokenRepository.save(token);
    }

    // Generates a long random token for refresh-token cookies.
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    // Hashes refresh tokens before storage or lookup.
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

}

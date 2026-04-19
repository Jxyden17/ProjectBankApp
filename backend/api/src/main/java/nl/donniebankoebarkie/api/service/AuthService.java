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

    @Override
    public UserResponse register(RegisterRequest request) {
        if (authRepository.existsByEmail(request.email())) {
            throw new ConflictException("A user with that email already exists.");
        }
        if (authRepository.existsByBsnNumber(request.bsnNumber())) {
            throw new ConflictException("A user with that BSN already exists.");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User(
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
        return toUserResponse(authRepository.save(user));
    }

    @Override
    public LoginResult login(LoginRequest request) {
        User user = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateRefreshToken();
        saveRefreshToken(user.getId(), rawRefreshToken);

        return new LoginResult(
                new AuthResponse(accessToken, "Bearer", jwtProperties.accessTokenExpirationSeconds(), toUserResponse(user)),
                rawRefreshToken,
                jwtProperties.refreshTokenExpirationSeconds()
        );
    }

    @Override
    public RefreshResult refresh(String refreshToken) {
        String sanitizedRefreshToken = refreshToken == null ? null : refreshToken.trim();

        if (sanitizedRefreshToken == null || sanitizedRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing or invalid.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(sanitizedRefreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is missing or invalid."));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked.");
        }

        User user = authRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token user was not found."));

        refreshTokenRepository.revoke(storedToken);
        String rotatedRefreshToken = generateRefreshToken();
        saveRefreshToken(user.getId(), rotatedRefreshToken);

        return new RefreshResult(
                new RefreshResponse(jwtService.generateAccessToken(user), "Bearer", jwtProperties.accessTokenExpirationSeconds()),
                rotatedRefreshToken,
                jwtProperties.refreshTokenExpirationSeconds()
        );
    }

    @Override
    public void logout(String refreshToken) {
        String sanitizedRefreshToken = refreshToken == null ? null : refreshToken.trim();

        if (sanitizedRefreshToken == null || sanitizedRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(sanitizedRefreshToken))
                .ifPresent(refreshTokenRepository::revoke);
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        return authRepository.findById(userId)
                .map(this::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found."));
    }

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

    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBsnNumber(),
                user.getRole(),
                user.isApproved(),
                user.getApprovedAt(),
                user.getApprovedByUserId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

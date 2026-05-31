package nl.donniebankoebarkie.api.service.result;

import nl.donniebankoebarkie.api.dto.auth.response.AuthResponse;

public record LoginResult(
        AuthResponse response,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}

package nl.donniebankoebarkie.api.dto.auth.response;

import nl.donniebankoebarkie.api.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}

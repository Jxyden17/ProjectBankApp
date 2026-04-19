package nl.donniebankoebarkie.api.dto.auth.response;

public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

package nl.donniebankoebarkie.api.service.result;

import nl.donniebankoebarkie.api.dto.auth.response.RefreshResponse;

public record RefreshResult(
        RefreshResponse response,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}

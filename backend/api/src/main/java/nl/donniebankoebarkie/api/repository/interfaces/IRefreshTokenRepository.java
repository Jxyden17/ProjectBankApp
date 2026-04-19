package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.RefreshToken;

import java.util.Optional;

public interface IRefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revoke(RefreshToken refreshToken);
}

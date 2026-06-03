package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    default void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        save(refreshToken);
    }
}

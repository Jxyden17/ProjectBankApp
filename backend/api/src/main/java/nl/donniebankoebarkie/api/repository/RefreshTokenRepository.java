package nl.donniebankoebarkie.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import nl.donniebankoebarkie.api.model.RefreshToken;
import nl.donniebankoebarkie.api.repository.interfaces.IRefreshTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class RefreshTokenRepository implements IRefreshTokenRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        if (refreshToken.getId() == null) {
            entityManager.persist(refreshToken);
            return refreshToken;
        }

        return entityManager.merge(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        try {
            return Optional.of(entityManager.createQuery(
                            "select rt from RefreshToken rt where rt.tokenHash = :tokenHash", RefreshToken.class)
                    .setParameter("tokenHash", tokenHash)
                    .getSingleResult());
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        save(refreshToken);
    }
}

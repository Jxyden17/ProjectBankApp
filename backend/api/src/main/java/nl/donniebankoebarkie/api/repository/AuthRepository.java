package nl.donniebankoebarkie.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class AuthRepository implements IAuthRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            return Optional.of(entityManager.createQuery(
                            "select u from User u where u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult());
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByBsnNumber(String bsnNumber) {
        Long count = entityManager.createQuery(
                        "select count(u) from User u where u.bsnNumber = :bsnNumber", Long.class)
                .setParameter("bsnNumber", bsnNumber)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }

        return entityManager.merge(user);
    }
}

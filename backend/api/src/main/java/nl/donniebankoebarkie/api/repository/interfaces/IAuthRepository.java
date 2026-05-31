package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.User;

import java.util.Optional;

public interface IAuthRepository {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByBsnNumber(String bsnNumber);

    User save(User user);
}

package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.User;

import java.util.List;
import java.util.Optional;

public interface IUserRepository {
    List<User> findAll();

    Optional<User> findById(Long id);

    User save(User user);
}

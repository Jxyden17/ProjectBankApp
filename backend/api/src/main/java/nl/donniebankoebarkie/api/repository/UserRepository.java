package nl.donniebankoebarkie.api.repository;

import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.repository.interfaces.IUserJpaRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IUserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository implements IUserRepository {
    private final IUserJpaRepository userJpaRepository;

    public UserRepository(IUserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }
}

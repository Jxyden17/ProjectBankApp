package nl.donniebankoebarkie.api.repository.interfaces;

import nl.donniebankoebarkie.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserJpaRepository extends JpaRepository<User, Long> {
}

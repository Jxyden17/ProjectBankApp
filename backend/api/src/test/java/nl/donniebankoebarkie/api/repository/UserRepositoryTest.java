package nl.donniebankoebarkie.api.repository;

import nl.donniebankoebarkie.api.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAssignsAnIdToNewUsers() {
        User savedUser = userRepository.save(new User(null, "Charlie", "Student", "charlie@example.com"));

        assertTrue(savedUser.getId() != null);
        assertEquals("Charlie", savedUser.getFirstName());
    }
}

package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.UserRequest;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsersMapsRepositoryModelsToResponses() {
        when(userRepository.findAll()).thenReturn(List.of(
                new User(1L, "Alice", "Example", "alice@example.com"),
                new User(2L, "Bob", "Example", "bob@example.com")
        ));

        List<UserResponse> users = userService.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("alice@example.com", users.getFirst().email());
    }

    @Test
    void getUserByIdReturnsMappedUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                new User(1L, "Alice", "Example", "alice@example.com")
        ));

        UserResponse user = userService.getUserById(1L);

        assertEquals(1L, user.id());
        assertEquals("Alice", user.firstName());
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void createUserReturnsTheCreatedUser() {
        when(userRepository.save(any(User.class))).thenReturn(
                new User(3L, "Charlie", "Student", "charlie@example.com")
        );

        UserResponse createdUser = userService.createUser(new UserRequest(
                "Charlie",
                "Student",
                "charlie@example.com"
        ));

        assertEquals(3L, createdUser.id());
        assertEquals("Charlie", createdUser.firstName());
    }
}

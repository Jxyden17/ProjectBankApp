package nl.donniebankoebarkie.api.controller;

import nl.donniebankoebarkie.api.dto.UserRequest;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.service.interfaces.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getAllUsersReturnsUsersFromTheService() {
        when(userService.getAllUsers()).thenReturn(List.of(
                new UserResponse(1L, "Alice", "Example", "alice@example.com"),
                new UserResponse(2L, "Bob", "Example", "bob@example.com")
        ));

        List<UserResponse> users = userController.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("alice@example.com", users.getFirst().email());
    }

    @Test
    void createUserReturnsCreatedResponseWithApiLocation() {
        UserRequest request = new UserRequest("Charlie", "Student", "charlie@example.com");
        when(userService.createUser(request)).thenReturn(
                new UserResponse(3L, "Charlie", "Student", "charlie@example.com")
        );
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        try {
            var response = userController.createUser(request);

            assertEquals(201, response.getStatusCode().value());
            assertEquals("Charlie", response.getBody().firstName());
            assertTrue(response.getHeaders().getLocation() != null);
            assertEquals("http://localhost/api/users/3", response.getHeaders().getLocation().toString());
        }
        finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}

package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.UserRequest;
import nl.donniebankoebarkie.api.dto.UserResponse;

import java.util.List;

public interface IUserService {
    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);
}

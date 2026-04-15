package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.UserRequest;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.repository.interfaces.IUserRepository;
import nl.donniebankoebarkie.api.service.interfaces.IUserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IUserService {
    private final IUserRepository userRepository;

    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " was not found."));
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        User user = new User(null, request.firstName(), request.lastName(), request.email());
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}

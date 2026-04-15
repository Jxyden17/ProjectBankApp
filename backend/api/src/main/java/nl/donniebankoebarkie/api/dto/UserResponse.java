package nl.donniebankoebarkie.api.dto;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}

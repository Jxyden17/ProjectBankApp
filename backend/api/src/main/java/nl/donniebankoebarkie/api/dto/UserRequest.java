package nl.donniebankoebarkie.api.dto;

public record UserRequest(
        String firstName,
        String lastName,
        String email
) {
}

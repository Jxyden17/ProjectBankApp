package nl.donniebankoebarkie.api.dto.auth.request;

public record LoginRequest(
        String email,
        String password
) {
}

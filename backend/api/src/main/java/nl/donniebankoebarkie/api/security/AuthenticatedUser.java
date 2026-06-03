package nl.donniebankoebarkie.api.security;

import nl.donniebankoebarkie.api.model.enums.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role
) {
}

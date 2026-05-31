package nl.donniebankoebarkie.api.dto;

import nl.donniebankoebarkie.api.model.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String bsnNumber,
        UserRole role,
        boolean approved,
        LocalDateTime approvedAt,
        Long approvedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

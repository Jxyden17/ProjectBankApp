package nl.donniebankoebarkie.api.mapper;

import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerSummaryResponse;
import nl.donniebankoebarkie.api.model.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBsnNumber(),
                user.getRole(),
                user.isApproved(),
                user.getApprovedAt(),
                user.getApprovedByUserId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static CustomerSummaryResponse toCustomerSummaryResponse(User user) {
        return new CustomerSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isApproved()
        );
    }
}

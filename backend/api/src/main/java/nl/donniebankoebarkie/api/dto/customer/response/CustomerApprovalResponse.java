package nl.donniebankoebarkie.api.dto.customer.response;

import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.account.AccountResponse;

import java.util.List;

public record CustomerApprovalResponse(
        UserResponse customer,
        List<AccountResponse> accounts
) {
}

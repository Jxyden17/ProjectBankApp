package nl.donniebankoebarkie.api.dto.account;

import nl.donniebankoebarkie.api.dto.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public record AccountOverviewResponse(
        UserResponse customer,
        List<AccountResponse> accounts,
        BigDecimal combinedBalance
) {
}

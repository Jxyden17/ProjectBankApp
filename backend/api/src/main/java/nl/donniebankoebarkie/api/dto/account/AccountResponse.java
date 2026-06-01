package nl.donniebankoebarkie.api.dto.account;

import nl.donniebankoebarkie.api.model.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String iban,
        Long userId,
        AccountType accountType,
        BigDecimal balance,
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

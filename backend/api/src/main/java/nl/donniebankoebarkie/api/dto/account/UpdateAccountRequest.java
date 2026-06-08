package nl.donniebankoebarkie.api.dto.account;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateAccountRequest(
        BigDecimal absoluteTransferLimit,
        @DecimalMin(value = "0.00", message = "dailyTransferLimit must be zero or greater.")
        BigDecimal dailyTransferLimit,
        Boolean isActive
) {
}

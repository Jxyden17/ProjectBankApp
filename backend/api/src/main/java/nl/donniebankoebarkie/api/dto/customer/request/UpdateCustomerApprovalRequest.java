package nl.donniebankoebarkie.api.dto.customer.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCustomerApprovalRequest(
        @NotNull(message = "approved is required.") Boolean approved,
        @NotNull(message = "checkingAbsoluteTransferLimit is required.") BigDecimal checkingAbsoluteTransferLimit,
        @NotNull(message = "checkingDailyTransferLimit is required.")
        @DecimalMin(value = "0.00", message = "checkingDailyTransferLimit must be zero or greater.")
        BigDecimal checkingDailyTransferLimit,
        @NotNull(message = "savingsAbsoluteTransferLimit is required.")
        @DecimalMin(value = "0.00", message = "savingsAbsoluteTransferLimit must be zero or greater.")
        BigDecimal savingsAbsoluteTransferLimit,
        @NotNull(message = "savingsDailyTransferLimit is required.")
        @DecimalMin(value = "0.00", message = "savingsDailyTransferLimit must be zero or greater.")
        BigDecimal savingsDailyTransferLimit
) {
}

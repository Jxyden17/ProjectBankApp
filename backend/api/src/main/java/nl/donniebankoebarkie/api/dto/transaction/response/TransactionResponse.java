package nl.donniebankoebarkie.api.dto.transaction.response;

import nl.donniebankoebarkie.api.model.enums.Channel;
import nl.donniebankoebarkie.api.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String currency,
        TransactionType transactionType,
        Long initiatedByUserId,
        Channel channel,
        LocalDateTime timestamp,
        String description
) {
}

package nl.donniebankoebarkie.api.dto.transaction.request;

import nl.donniebankoebarkie.api.model.enums.Channel;

import java.math.BigDecimal;

public record DepositTransactionRequest(
        Long toAccountId,
        BigDecimal amount,
        Channel channel,
        String description
) {
}

package nl.donniebankoebarkie.api.dto.transaction.request;

import nl.donniebankoebarkie.api.model.enums.Channel;

import java.math.BigDecimal;

public record TransferTransactionRequest(
        Long fromAccountId,
        Long toAccountId,
        String destinationIban,
        BigDecimal amount,
        Channel channel,
        String description
) {
}

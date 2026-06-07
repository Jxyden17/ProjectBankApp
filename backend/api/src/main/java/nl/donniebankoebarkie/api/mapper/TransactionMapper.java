package nl.donniebankoebarkie.api.mapper;

import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.model.Transaction;

public class TransactionMapper {
    public static TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionType(),
                transaction.getInitiatedByUserId(),
                transaction.getChannel(),
                transaction.getTimestamp(),
                transaction.getDescription()
        );
    }
}

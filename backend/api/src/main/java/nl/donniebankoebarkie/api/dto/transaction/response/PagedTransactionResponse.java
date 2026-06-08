package nl.donniebankoebarkie.api.dto.transaction.response;

import nl.donniebankoebarkie.api.dto.PageMetadata;

import java.util.List;

public record PagedTransactionResponse(
        List<TransactionResponse> items,
        PageMetadata page
) {
}

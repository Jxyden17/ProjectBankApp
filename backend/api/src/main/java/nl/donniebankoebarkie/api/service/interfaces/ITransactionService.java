package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;

public interface ITransactionService {
    PagedTransactionResponse listPendingTransactions(int page, int size);
}

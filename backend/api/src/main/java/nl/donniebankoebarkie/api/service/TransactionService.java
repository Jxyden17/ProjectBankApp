package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.service.interfaces.ITransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TransactionService implements ITransactionService {
    private final ITransactionService transactionService;

    public TransactionService(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public PagedTransactionResponse listPendingTransactions(int page, int size) {
//        int sanitizedPage = Math.max(page, 0);
//        int sanitizedSize = Math.min(Math.max(size, 1), 100);
//        PageRequest pageRequest = PageRequest.of(sanitizedPage, sanitizedSize);
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

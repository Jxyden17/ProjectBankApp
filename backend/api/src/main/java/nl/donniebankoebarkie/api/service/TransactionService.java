package nl.donniebankoebarkie.api.service;

import jakarta.persistence.criteria.Predicate;
import nl.donniebankoebarkie.api.dto.PageMetadata;
import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.mapper.TransactionMapper;
import nl.donniebankoebarkie.api.model.Transaction;
import nl.donniebankoebarkie.api.model.enums.TransactionType;
import nl.donniebankoebarkie.api.repository.interfaces.ITransactionRepository;
import nl.donniebankoebarkie.api.service.interfaces.ITransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class TransactionService implements ITransactionService {
    private final ITransactionRepository transactionRepository;

    public TransactionService(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public PagedTransactionResponse listTransactions(
            LocalDate startDate, LocalDate endDate,
            BigDecimal amountEq, BigDecimal amountLt,
            BigDecimal amountGt, String iban, Long customerId,
            TransactionType transactionType, int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(sanitizedPage, sanitizedSize,
                Sort.by(Sort.Order.desc("timestamp")));

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) { predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay())); }
            if (endDate != null) { predicates.add(cb.lessThan(root.get("timestamp"), endDate.plusDays(1).atStartOfDay())); }
            if (amountEq != null) { predicates.add(cb.equal(root.get("amount"), amountEq)); }
            if (amountLt != null) { predicates.add(cb.lessThan(root.get("amount"), amountLt)); }
            if (amountGt != null) { predicates.add(cb.greaterThan(root.get("amount"), amountGt)); }
            if (transactionType != null) { predicates.add(cb.equal(root.get("transactionType"), transactionType)); }
            if (customerId != null) { predicates.add(cb.equal(root.get("initiatedByUserId"), customerId)); }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageRequest);

        List<TransactionResponse> transactions = transactionPage.getContent()
                .stream()
                .map(TransactionMapper::toTransactionResponse)
                .toList();

        return new PagedTransactionResponse(
                transactions,
                new PageMetadata(
                        transactionPage.getNumber(),
                        transactionPage.getSize(),
                        transactionPage.getTotalElements(),
                        transactionPage.getTotalPages()
                )
        );
    }

    @Override
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return TransactionMapper.toTransactionResponse(transaction);
    }

    @Override
    public TransactionResponse createTransfer(TransferTransactionRequest request, Long initiatedByUserId) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.fromAccountId());
        transaction.setToAccountId(request.toAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(initiatedByUserId);
        transaction.setTimestamp(LocalDateTime.now());
        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createDeposit(DepositTransactionRequest request, Long initiatedByUserId) {
        Transaction transaction = new Transaction();
        transaction.setToAccountId(request.toAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(initiatedByUserId);
        transaction.setTimestamp(LocalDateTime.now());
        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createWithdrawal(WithdrawalTransactionRequest request, Long initiatedByUserId) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.fromAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(initiatedByUserId);
        transaction.setTimestamp(LocalDateTime.now());
        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

}

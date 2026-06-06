package nl.donniebankoebarkie.api.service;

import jakarta.persistence.criteria.Predicate;
import nl.donniebankoebarkie.api.dto.PageMetadata;
import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.exception.BadRequestException;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.mapper.TransactionMapper;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.Transaction;
import nl.donniebankoebarkie.api.model.enums.TransactionType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.ITransactionRepository;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.ITransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TransactionService implements ITransactionService {
    private final ITransactionRepository transactionRepository;
    private final IAccountRepository accountRepository;

    public TransactionService(ITransactionRepository transactionRepository, IAccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
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
            if (iban != null && !iban.isBlank()) {
                var fromAccountSubquery = query.subquery(Long.class);
                var fromAccountRoot = fromAccountSubquery.from(Account.class);
                fromAccountSubquery.select(fromAccountRoot.get("id"))
                        .where(cb.equal(fromAccountRoot.get("iban"), iban));

                var toAccountSubquery = query.subquery(Long.class);
                var toAccountRoot = toAccountSubquery.from(Account.class);
                toAccountSubquery.select(toAccountRoot.get("id"))
                        .where(cb.equal(toAccountRoot.get("iban"), iban));

                predicates.add(cb.or(
                        root.get("fromAccountId").in(fromAccountSubquery),
                        root.get("toAccountId").in(toAccountSubquery)
                ));
            }

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
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId, AuthenticatedUser authenticatedUser) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (authenticatedUser.role() != UserRole.EMPLOYEE) {
            assertCustomerOwnsTransaction(transaction, authenticatedUser.userId());
        }

        return TransactionMapper.toTransactionResponse(transaction);
    }

    @Override
    public TransactionResponse createTransfer(TransferTransactionRequest request, AuthenticatedUser authenticatedUser) {
        validateTransferRequest(request);

        Account fromAccount = getActiveAccount(request.fromAccountId());

        if (authenticatedUser.role() != UserRole.EMPLOYEE) {
            assertAccountOwnedByUser(fromAccount, authenticatedUser.userId());
        }

        Long resolvedToAccountId = resolveToAccountId(request);

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(fromAccount.getId());
        transaction.setToAccountId(resolvedToAccountId);
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(LocalDateTime.now());

        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createDeposit(DepositTransactionRequest request, AuthenticatedUser authenticatedUser) {
        Account toAccount = getActiveAccount(request.toAccountId());

        Transaction transaction = new Transaction();
        transaction.setToAccountId(toAccount.getId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(LocalDateTime.now());

        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createWithdrawal(WithdrawalTransactionRequest request, AuthenticatedUser authenticatedUser) {
        Account fromAccount = getActiveAccount(request.fromAccountId());

        if (authenticatedUser.role() != UserRole.EMPLOYEE) {
            assertAccountOwnedByUser(fromAccount, authenticatedUser.userId());
        }

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(fromAccount.getId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(LocalDateTime.now());

        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    private Account getActiveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (!account.isActive()) {
            throw new BadRequestException("Account is not active.");
        }
        return account;
    }

    private void assertAccountOwnedByUser(Account account, Long userId) {
        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this account.");
        }
    }

    private void assertCustomerOwnsTransaction(Transaction transaction, Long userId) {
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        boolean involved = userAccounts.stream()
                .map(Account::getId)
                .anyMatch(id -> id.equals(transaction.getFromAccountId())
                        || id.equals(transaction.getToAccountId()));
        if (!involved) {
            throw new ResourceNotFoundException("Transaction not found.");
        }
    }

    private void validateTransferRequest(TransferTransactionRequest request) {
        boolean hasToAccountId = request.toAccountId() != null;
        boolean hasDestinationIban = request.destinationIban() != null && !request.destinationIban().isBlank();

        if (!hasToAccountId && !hasDestinationIban) {
            throw new BadRequestException("Either toAccountId or destinationIban must be provided.");
        }
        if (hasToAccountId && hasDestinationIban) {
            throw new BadRequestException("Provide either toAccountId or destinationIban, not both.");
        }
    }

    private Long resolveToAccountId(TransferTransactionRequest request) {
        if (request.toAccountId() != null) {
            Account toAccount = getActiveAccount(request.toAccountId());
            return toAccount.getId();
        }

        return accountRepository.findAll().stream()
                .filter(a -> a.getIban().equalsIgnoreCase(request.destinationIban()))
                .map(Account::getId)
                .findFirst()
                .orElse(null);
    }
}
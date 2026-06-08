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
import nl.donniebankoebarkie.api.model.enums.AccountType;
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
            TransactionType transactionType, int page, int size,
            AuthenticatedUser authenticatedUser) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(sanitizedPage, sanitizedSize,
                Sort.by(Sort.Order.desc("timestamp")));

        final boolean isCustomer = authenticatedUser.role() != UserRole.EMPLOYEE;
        final List<Long> ownAccountIds = isCustomer
                ? accountRepository.findByUserId(authenticatedUser.userId())
                .stream().map(Account::getId).toList()
                : null;
        final String normalizedIban = iban == null || iban.isBlank() ? null : iban.trim();

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isCustomer) {
                if (ownAccountIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.or(
                            root.get("fromAccountId").in(ownAccountIds),
                            root.get("toAccountId").in(ownAccountIds)
                    ));
                }
            } else {
                if (customerId != null) { predicates.add(cb.equal(root.get("initiatedByUserId"), customerId)); }
            }

            if (normalizedIban != null) {
                predicates.add(ibanPredicate(root, query, cb, normalizedIban));
            }

            if (startDate != null) { predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay())); }
            if (endDate != null) { predicates.add(cb.lessThan(root.get("timestamp"), endDate.plusDays(1).atStartOfDay())); }
            if (amountEq != null) { predicates.add(cb.equal(root.get("amount"), amountEq)); }
            if (amountLt != null) { predicates.add(cb.lessThan(root.get("amount"), amountLt)); }
            if (amountGt != null) { predicates.add(cb.greaterThan(root.get("amount"), amountGt)); }
            if (transactionType != null) { predicates.add(cb.equal(root.get("transactionType"), transactionType)); }

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
        validateMoneyMovementRequest(request.amount(), request.channel());
        validateTransferRequest(request);

        Account fromAccount = getActiveAccount(request.fromAccountId());

        if (authenticatedUser.role() != UserRole.EMPLOYEE) {
            assertAccountOwnedByUser(fromAccount, authenticatedUser.userId());
        }

        Account toAccount = resolveDestinationAccount(request);
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BadRequestException("Source and destination accounts must be different.");
        }

        boolean crossUserTransfer = !fromAccount.getUserId().equals(toAccount.getUserId());
        if (authenticatedUser.role() == UserRole.EMPLOYEE || crossUserTransfer) {
            assertCheckingAccount(fromAccount, "Transfers between different customer accounts must use checking accounts.");
            assertCheckingAccount(toAccount, "Transfers between different customer accounts must use checking accounts.");
        }

        enforceTransferLimits(fromAccount, request.amount());
        LocalDateTime now = LocalDateTime.now();
        applyDebit(fromAccount, request.amount(), now);
        applyCredit(toAccount, request.amount(), now);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(fromAccount.getId());
        transaction.setToAccountId(toAccount.getId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(now);

        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createDeposit(DepositTransactionRequest request, AuthenticatedUser authenticatedUser) {
        validateMoneyMovementRequest(request.amount(), request.channel());
        Account toAccount = getActiveAccount(request.toAccountId());
        LocalDateTime now = LocalDateTime.now();

        applyCredit(toAccount, request.amount(), now);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setToAccountId(toAccount.getId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(now);

        return TransactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse createWithdrawal(WithdrawalTransactionRequest request, AuthenticatedUser authenticatedUser) {
        validateMoneyMovementRequest(request.amount(), request.channel());
        Account fromAccount = getActiveAccount(request.fromAccountId());

        if (authenticatedUser.role() != UserRole.EMPLOYEE) {
            assertAccountOwnedByUser(fromAccount, authenticatedUser.userId());
        }

        enforceTransferLimits(fromAccount, request.amount());
        LocalDateTime now = LocalDateTime.now();
        applyDebit(fromAccount, request.amount(), now);
        accountRepository.save(fromAccount);

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(fromAccount.getId());
        transaction.setAmount(request.amount());
        transaction.setCurrency("EUR");
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setChannel(request.channel());
        transaction.setDescription(request.description());
        transaction.setInitiatedByUserId(authenticatedUser.userId());
        transaction.setTimestamp(now);

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

    private void assertCheckingAccount(Account account, String message) {
        if (account.getAccountType() != AccountType.CHECKING) {
            throw new BadRequestException(message);
        }
    }

    private void validateMoneyMovementRequest(BigDecimal amount, nl.donniebankoebarkie.api.model.enums.Channel channel) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero.");
        }
        if (channel == null) {
            throw new BadRequestException("Channel is required.");
        }
    }

    private void enforceTransferLimits(Account account, BigDecimal amount) {
        BigDecimal projectedBalance = account.getBalance().subtract(amount);
        if (projectedBalance.compareTo(account.getAbsoluteTransferLimit()) < 0) {
            throw new BadRequestException("Transfer limit exceeded.");
        }

        BigDecimal dailyOutgoing = transactionRepository.findAll().stream()
                .filter(transaction -> account.getId().equals(transaction.getFromAccountId()))
                .filter(transaction -> transaction.getTimestamp() != null)
                .filter(transaction -> transaction.getTimestamp().toLocalDate().equals(LocalDate.now()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (dailyOutgoing.add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            throw new BadRequestException("Daily transfer limit exceeded.");
        }
    }

    private void applyDebit(Account account, BigDecimal amount, LocalDateTime timestamp) {
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(timestamp);
    }

    private void applyCredit(Account account, BigDecimal amount, LocalDateTime timestamp) {
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(timestamp);
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

    private Predicate ibanPredicate(
            jakarta.persistence.criteria.Root<Transaction> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String iban) {
        var fromSub = query.subquery(Long.class);
        var fromRoot = fromSub.from(Account.class);
        fromSub.select(fromRoot.get("id")).where(cb.equal(fromRoot.get("iban"), iban));

        var toSub = query.subquery(Long.class);
        var toRoot = toSub.from(Account.class);
        toSub.select(toRoot.get("id")).where(cb.equal(toRoot.get("iban"), iban));

        return cb.or(
                root.get("fromAccountId").in(fromSub),
                root.get("toAccountId").in(toSub)
        );
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

    private Account resolveDestinationAccount(TransferTransactionRequest request) {
        if (request.toAccountId() != null) {
            return getActiveAccount(request.toAccountId());
        }

        return accountRepository.findAll().stream()
                .filter(a -> a.getIban().equalsIgnoreCase(request.destinationIban().trim()))
                .findFirst()
                .map(account -> {
                    if (!account.isActive()) {
                        throw new BadRequestException("Account is not active.");
                    }
                    return account;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found."));
    }
}

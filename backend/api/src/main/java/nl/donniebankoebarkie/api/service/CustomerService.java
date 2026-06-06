package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.PageMetadata;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerSummaryResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerSummaryResponse;
import nl.donniebankoebarkie.api.exception.BadRequestException;
import nl.donniebankoebarkie.api.exception.ConflictException;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.mapper.AccountMapper;
import nl.donniebankoebarkie.api.mapper.UserMapper;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.AccountType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import nl.donniebankoebarkie.api.service.interfaces.ICustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService implements ICustomerService {
    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 20;
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;

    private final IAuthRepository authRepository;
    private final IAccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerService(IAuthRepository authRepository, IAccountRepository accountRepository) {
        this.authRepository = authRepository;
        this.accountRepository = accountRepository;
    }

    // Returns pending customers with bounded pagination to avoid overly large pages.
    @Override
    @Transactional(readOnly = true)
    public PagedCustomerSummaryResponse listPendingCustomers(int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(
                sanitizedPage,
                sanitizedSize,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        );
        Page<User> pendingCustomerPage = authRepository.findPendingCustomers(UserRole.CUSTOMER, pageRequest);

        List<CustomerSummaryResponse> customers = pendingCustomerPage.getContent()
                .stream()
                .map(UserMapper::toCustomerSummaryResponse)
                .toList();

        return new PagedCustomerSummaryResponse(
                customers,
                new PageMetadata(
                        pendingCustomerPage.getNumber(),
                        pendingCustomerPage.getSize(),
                        pendingCustomerPage.getTotalElements(),
                        pendingCustomerPage.getTotalPages()
                )
        );
    }

    // Approves a customer and creates both accounts in one transaction.
    @Override
    @Transactional
    public CustomerApprovalResponse approveCustomer(
            Long customerId,
            Long approvedByUserId,
            UpdateCustomerApprovalRequest request
    ) {
        validateApprovalRequest(request);
        User customer = getPendingCustomer(customerId);
        LocalDateTime now = LocalDateTime.now();

        User approvedCustomer = approveCustomerRecord(customer, approvedByUserId, now);
        Account checkingAccount = createAccount(
                approvedCustomer.getId(),
                AccountType.CHECKING,
                request.checkingAbsoluteTransferLimit(),
                request.checkingDailyTransferLimit(),
                now
        );
        Account savingsAccount = createAccount(
                approvedCustomer.getId(),
                AccountType.SAVINGS,
                request.savingsAbsoluteTransferLimit(),
                request.savingsDailyTransferLimit(),
                now
        );

        return new CustomerApprovalResponse(
                UserMapper.toUserResponse(approvedCustomer),
                List.of(AccountMapper.toAccountResponse(checkingAccount), AccountMapper.toAccountResponse(savingsAccount))
        );
    }

    // Ensures this endpoint is only used for approval, not rejection.
    private void validateApprovalRequest(UpdateCustomerApprovalRequest request) {
        if (!Boolean.TRUE.equals(request.approved())) {
            throw new BadRequestException("Customer approval must be true.");
        }
    }

    // Loads an unapproved customer and rejects employees or already approved users.
    private User getPendingCustomer(Long customerId) {
        User customer = authRepository.findById(customerId)
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer was not found."));

        if (customer.isApproved()) {
            throw new ConflictException("Customer is already approved.");
        }

        return customer;
    }

    // Marks the customer as approved and records the approving employee.
    private User approveCustomerRecord(User customer, Long approvedByUserId, LocalDateTime approvedAt) {
        customer.setApproved(true);
        customer.setApprovedByUserId(approvedByUserId);
        customer.setApprovedAt(approvedAt);
        customer.setUpdatedAt(approvedAt);

        return authRepository.save(customer);
    }

    // Persists a new account for the approved customer.
    private Account createAccount(
            Long userId,
            AccountType accountType,
            BigDecimal absoluteTransferLimit,
            BigDecimal dailyTransferLimit,
            LocalDateTime now
    ) {
        return accountRepository.save(newAccount(
                userId,
                accountType,
                absoluteTransferLimit,
                dailyTransferLimit,
                now
        ));
    }

    // Builds a zero-balance active account with the requested limits.
    private Account newAccount(
            Long userId,
            AccountType accountType,
            BigDecimal absoluteTransferLimit,
            BigDecimal dailyTransferLimit,
            LocalDateTime now
    ) {
        Account account = new Account();
        account.setIban(generateUniqueIban());
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setBalance(INITIAL_BALANCE);
        account.setAbsoluteTransferLimit(absoluteTransferLimit);
        account.setDailyTransferLimit(dailyTransferLimit);
        account.setActive(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }

    // Tries a limited number of random IBANs to avoid an endless collision loop.
    private String generateUniqueIban() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            String iban = "NL%02dINHO%010d".formatted(
                    secureRandom.nextInt(100),
                    secureRandom.nextLong(10_000_000_000L)
            );

            if (!accountRepository.existsByIban(iban)) {
                return iban;
            }
        }

        throw new ConflictException("Could not generate a unique IBAN.");
    }

}

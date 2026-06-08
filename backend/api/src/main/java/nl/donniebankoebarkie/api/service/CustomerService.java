package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.PageMetadata;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.customer.request.CustomerSearchRequest;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerIbanLookupResponse;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerSummaryResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerIbanLookupResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService implements ICustomerService {
    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 20;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;
    private static final Sort DEFAULT_CUSTOMER_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private static final Sort DEFAULT_PENDING_CUSTOMER_SORT = Sort.by(Sort.Order.asc("createdAt"),
            Sort.Order.asc("id"));

    private final IAuthRepository authRepository;
    private final IAccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerService(IAuthRepository authRepository, IAccountRepository accountRepository) {
        this.authRepository = authRepository;
        this.accountRepository = accountRepository;
    }

    // Lists customers for employees with optional filters and bounded pagination.
    @Override
    @Transactional(readOnly = true)
    public PagedCustomerSummaryResponse listCustomers(CustomerSearchRequest request, Pageable pageable) {
        Page<User> customerPage = authRepository.findAll(
                customerSpecification(request),
                sanitizePageable(pageable, DEFAULT_CUSTOMER_SORT));

        return toPagedCustomerSummaryResponse(customerPage);
    }

    // Lists only unapproved customers while still allowing employee search filters.
    @Override
    @Transactional(readOnly = true)
    public PagedCustomerSummaryResponse listPendingCustomers(CustomerSearchRequest request, Pageable pageable) {
        CustomerSearchRequest pendingRequest = new CustomerSearchRequest(
                false,
                request.firstName(),
                request.lastName(),
                request.email());
        Page<User> pendingCustomerPage = authRepository.findAll(
                customerSpecification(pendingRequest),
                sanitizePageable(pageable, DEFAULT_PENDING_CUSTOMER_SORT));

        return toPagedCustomerSummaryResponse(pendingCustomerPage);
    }

    // Returns full customer profile data for employee customer detail views.
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCustomer(Long customerId) {
        return authRepository.findById(customerId)
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .map(UserMapper::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer was not found."));
    }

    // Looks up active customer IBANs by exact first and last name without exposing
    // full profiles.
    @Override
    @Transactional(readOnly = true)
    public PagedCustomerIbanLookupResponse lookupCustomerIbans(String firstName, String lastName, Pageable pageable) {
        String sanitizedFirstName = requireSearchValue(firstName, "First name is required.");
        String sanitizedLastName = requireSearchValue(lastName, "Last name is required.");

        Page<Account> lookupPage = accountRepository
                .findByActiveTrueAndUser_RoleAndUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase(
                        UserRole.CUSTOMER,
                        sanitizedFirstName,
                        sanitizedLastName,
                        sanitizePageable(pageable, Sort.unsorted()));

        return new PagedCustomerIbanLookupResponse(
                lookupPage.getContent()
                        .stream()
                        .map(this::toCustomerIbanLookupResponse)
                        .toList(),
                toPageMetadata(lookupPage));
    }

    // Approves a customer and creates both accounts in one transaction.
    @Override
    @Transactional
    public CustomerApprovalResponse approveCustomer(
            Long customerId,
            Long approvedByUserId,
            UpdateCustomerApprovalRequest request) {
        validateApprovalRequest(request);
        User customer = getPendingCustomer(customerId);
        LocalDateTime now = LocalDateTime.now();

        User approvedCustomer = approveCustomerRecord(customer, approvedByUserId, now);
        Account checkingAccount = createAccount(
                approvedCustomer.getId(),
                AccountType.CHECKING,
                request.checkingAbsoluteTransferLimit(),
                request.checkingDailyTransferLimit(),
                now);
        Account savingsAccount = createAccount(
                approvedCustomer.getId(),
                AccountType.SAVINGS,
                request.savingsAbsoluteTransferLimit(),
                request.savingsDailyTransferLimit(),
                now);

        return new CustomerApprovalResponse(
                UserMapper.toUserResponse(approvedCustomer),
                List.of(AccountMapper.toAccountResponse(checkingAccount),
                        AccountMapper.toAccountResponse(savingsAccount)));
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
            LocalDateTime now) {
        return accountRepository.save(newAccount(
                userId,
                accountType,
                absoluteTransferLimit,
                dailyTransferLimit,
                now));
    }

    // Builds a zero-balance active account with the requested limits.
    private Account newAccount(
            Long userId,
            AccountType accountType,
            BigDecimal absoluteTransferLimit,
            BigDecimal dailyTransferLimit,
            LocalDateTime now) {
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
                    secureRandom.nextLong(10_000_000_000L));

            if (!accountRepository.existsByIban(iban)) {
                return iban;
            }
        }

        throw new ConflictException("Could not generate a unique IBAN.");
    }

    private PagedCustomerSummaryResponse toPagedCustomerSummaryResponse(Page<User> customerPage) {
        List<CustomerSummaryResponse> customers = customerPage.getContent()
                .stream()
                .map(UserMapper::toCustomerSummaryResponse)
                .toList();

        return new PagedCustomerSummaryResponse(customers, toPageMetadata(customerPage));
    }

    // Copies Spring page metadata into the API response shape.
    private PageMetadata toPageMetadata(Page<?> page) {
        return new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    // Applies default sorting and caps page size to the OpenAPI maximum.
    private Pageable sanitizePageable(Pageable pageable, Sort defaultSort) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, defaultSort);
        }

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;

        return PageRequest.of(page, size, sort);
    }

    // Builds dynamic customer filters for Spring Data JPA.
    private Specification<User> customerSpecification(CustomerSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("role"), UserRole.CUSTOMER));

            if (request.approved() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isApproved"), request.approved()));
            }
            addContainsFilter(predicates, criteriaBuilder, root.get("firstName"), request.firstName());
            addContainsFilter(predicates, criteriaBuilder, root.get("lastName"), request.lastName());
            addContainsFilter(predicates, criteriaBuilder, root.get("email"), request.email());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Adds a case-insensitive contains filter when a search value is provided.
    private void addContainsFilter(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Expression<String> expression,
            String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(expression),
                "%" + value.trim().toLowerCase() + "%"));
    }

    // Requires lookup fields before querying customer IBANs.
    private String requireSearchValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }

        return value.trim();
    }

    // Maps an account with its loaded user into the lightweight lookup response.
    private CustomerIbanLookupResponse toCustomerIbanLookupResponse(Account account) {
        User customer = account.getUser();

        return new CustomerIbanLookupResponse(
                customer.getFirstName(),
                customer.getLastName(),
                account.getIban());
    }

}

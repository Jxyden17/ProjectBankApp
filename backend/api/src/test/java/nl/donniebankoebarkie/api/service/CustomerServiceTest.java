package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.customer.request.CustomerSearchRequest;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.exception.BadRequestException;
import nl.donniebankoebarkie.api.exception.ConflictException;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.AccountType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private IAuthRepository authRepository;

    @Mock
    private IAccountRepository accountRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void listPendingCustomersMapsPageMetadata() {
        User pendingCustomer = pendingCustomer(10L);
        when(authRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<User>>any(),
                any(Pageable.class)
        ))
                .thenReturn(new PageImpl<>(List.of(pendingCustomer), PageRequest.of(0, 20), 1));

        var response = customerService.listPendingCustomers(
                new CustomerSearchRequest(null, null, null, null),
                PageRequest.of(0, 20)
        );

        assertEquals(1, response.items().size());
        assertEquals(pendingCustomer.getId(), response.items().getFirst().id());
        assertEquals(0, response.page().page());
        assertEquals(20, response.page().size());
        assertEquals(1, response.page().totalElements());
        assertEquals(1, response.page().totalPages());
    }

    @Test
    void approveCustomerMarksCustomerApprovedAndCreatesCheckingAndSavingsAccounts() {
        User customer = pendingCustomer(20L);
        when(authRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(authRepository.save(customer)).thenReturn(customer);
        when(accountRepository.existsByIban(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerService.approveCustomer(customer.getId(), 99L, approvalRequest(true));

        assertTrue(response.customer().approved());
        assertEquals(99L, response.customer().approvedByUserId());
        assertEquals(2, response.accounts().size());
        assertEquals(AccountType.CHECKING, response.accounts().get(0).accountType());
        assertEquals(AccountType.SAVINGS, response.accounts().get(1).accountType());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());

        List<Account> savedAccounts = accountCaptor.getAllValues();
        assertEquals(AccountType.CHECKING, savedAccounts.get(0).getAccountType());
        assertEquals(new BigDecimal("-500.00"), savedAccounts.get(0).getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("1000.00"), savedAccounts.get(0).getDailyTransferLimit());
        assertEquals(AccountType.SAVINGS, savedAccounts.get(1).getAccountType());
        assertEquals(new BigDecimal("0.00"), savedAccounts.get(1).getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("5000.00"), savedAccounts.get(1).getDailyTransferLimit());
        assertEquals(BigDecimal.ZERO, savedAccounts.get(0).getBalance());
        assertTrue(savedAccounts.get(0).isActive());
    }

    @Test
    void approveCustomerRejectsFalseApprovalRequest() {
        assertThrows(
                BadRequestException.class,
                () -> customerService.approveCustomer(20L, 99L, approvalRequest(false))
        );

        verifyNoInteractions(authRepository, accountRepository);
    }

    @Test
    void approveCustomerRejectsMissingCustomer() {
        when(authRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.approveCustomer(20L, 99L, approvalRequest(true))
        );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void approveCustomerRejectsEmployeeUser() {
        User employee = pendingCustomer(20L);
        employee.setRole(UserRole.EMPLOYEE);
        when(authRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.approveCustomer(employee.getId(), 99L, approvalRequest(true))
        );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void approveCustomerRejectsAlreadyApprovedCustomer() {
        User customer = pendingCustomer(20L);
        customer.setApproved(true);
        when(authRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThrows(
                ConflictException.class,
                () -> customerService.approveCustomer(customer.getId(), 99L, approvalRequest(true))
        );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void approveCustomerFailsWhenUniqueIbanCannotBeGenerated() {
        User customer = pendingCustomer(20L);
        when(authRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(authRepository.save(customer)).thenReturn(customer);
        when(accountRepository.existsByIban(any())).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> customerService.approveCustomer(customer.getId(), 99L, approvalRequest(true))
        );
    }

    private User pendingCustomer(Long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 31, 12, 0);
        User user = new User();
        user.setId(id);
        user.setFirstName("Pending");
        user.setLastName("Customer");
        user.setEmail("pending.customer@example.com");
        user.setPasswordHash("encoded-password");
        user.setPhoneNumber("+31612345678");
        user.setBsnNumber("123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setApproved(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private UpdateCustomerApprovalRequest approvalRequest(boolean approved) {
        return new UpdateCustomerApprovalRequest(
                approved,
                new BigDecimal("-500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("5000.00")
        );
    }
}

package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;
import nl.donniebankoebarkie.api.dto.account.AccountResponse;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.AccountType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private IAuthRepository authRepository;

    @Mock
    private IAccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void getOwnAccountOverviewReturnsMappedAccountsAndCombinedBalance() {
        User customer = customer(7L);
        Account checking = account(1L, customer.getId(), AccountType.CHECKING, new BigDecimal("150.00"));
        Account savings = account(2L, customer.getId(), AccountType.SAVINGS, new BigDecimal("25.50"));
        when(authRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(accountRepository.findByUserId(customer.getId())).thenReturn(List.of(checking, savings));

        AccountOverviewResponse overview = accountService.getOwnAccountOverview(customer.getId());

        assertEquals(customer.getId(), overview.customer().id());
        assertEquals(2, overview.accounts().size());
        assertEquals("NL01INHO0000000001", overview.accounts().get(0).iban());
        assertEquals(AccountType.CHECKING, overview.accounts().get(0).accountType());
        assertEquals(new BigDecimal("175.50"), overview.combinedBalance());
    }

    @Test
    void getOwnAccountOverviewReturnsEmptyAccountsAndZeroCombinedBalance() {
        User customer = customer(8L);
        when(authRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(accountRepository.findByUserId(customer.getId())).thenReturn(List.of());

        AccountOverviewResponse overview = accountService.getOwnAccountOverview(customer.getId());

        assertTrue(overview.accounts().isEmpty());
        assertEquals(BigDecimal.ZERO, overview.combinedBalance());
    }

    @Test
    void getOwnAccountOverviewThrowsWhenUserNotFound() {
        when(authRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getOwnAccountOverview(99L));

        verify(accountRepository, never()).findByUserId(anyLong());
    }

    @Test
    void listAccountsReturnsAllAccountsForEmployees() {
        Account checking = account(1L, 7L, AccountType.CHECKING, new BigDecimal("150.00"));
        Account savings = account(2L, 8L, AccountType.SAVINGS, new BigDecimal("25.50"));
        when(accountRepository.findAll()).thenReturn(List.of(checking, savings));

        List<AccountResponse> accounts = accountService.listAccounts(99L, UserRole.EMPLOYEE);

        assertEquals(2, accounts.size());
        assertEquals("NL01INHO0000000001", accounts.get(0).iban());
        verify(accountRepository, never()).findByUserId(anyLong());
    }

    @Test
    void listAccountsReturnsOnlyOwnAccountsForCustomers() {
        Account own = account(1L, 7L, AccountType.CHECKING, new BigDecimal("150.00"));
        when(accountRepository.findByUserId(7L)).thenReturn(List.of(own));

        List<AccountResponse> accounts = accountService.listAccounts(7L, UserRole.CUSTOMER);

        assertEquals(1, accounts.size());
        assertEquals(7L, accounts.get(0).userId());
        verify(accountRepository, never()).findAll();
    }

    @Test
    void getAccountByIdReturnsAnyAccountForEmployees() {
        Account account = account(5L, 7L, AccountType.CHECKING, new BigDecimal("100.00"));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountById(5L, 99L, UserRole.EMPLOYEE);

        assertEquals(5L, response.id());
        assertEquals(7L, response.userId());
    }

    @Test
    void getAccountByIdReturnsOwnAccountForCustomers() {
        Account account = account(5L, 7L, AccountType.CHECKING, new BigDecimal("100.00"));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountById(5L, 7L, UserRole.CUSTOMER);

        assertEquals(5L, response.id());
    }

    @Test
    void getAccountByIdDeniesCustomersViewingAnotherUsersAccount() {
        Account account = account(5L, 7L, AccountType.CHECKING, new BigDecimal("100.00"));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThrows(
                AccessDeniedException.class,
                () -> accountService.getAccountById(5L, 8L, UserRole.CUSTOMER)
        );
    }

    @Test
    void getAccountByIdThrowsWhenAccountNotFound() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccountById(404L, 99L, UserRole.EMPLOYEE)
        );
    }

    private User customer(Long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 31, 12, 0);
        User user = new User();
        user.setId(id);
        user.setFirstName("Owner");
        user.setLastName("Customer");
        user.setEmail("owner.customer@example.com");
        user.setPasswordHash("encoded-password");
        user.setPhoneNumber("+31612345678");
        user.setBsnNumber("123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setApproved(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private Account account(Long id, Long userId, AccountType accountType, BigDecimal balance) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 31, 12, 0);
        Account account = new Account();
        account.setId(id);
        account.setIban("NL01INHO%010d".formatted(id));
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setBalance(balance);
        account.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        account.setDailyTransferLimit(new BigDecimal("1000.00"));
        account.setActive(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }
}

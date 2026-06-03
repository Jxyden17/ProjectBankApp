package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;
import nl.donniebankoebarkie.api.dto.account.AccountResponse;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.mapper.AccountMapper;
import nl.donniebankoebarkie.api.mapper.UserMapper;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import nl.donniebankoebarkie.api.service.interfaces.IAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService implements IAccountService {
    private final IAuthRepository authRepository;
    private final IAccountRepository accountRepository;

    public AccountService(IAuthRepository authRepository, IAccountRepository accountRepository) {
        this.authRepository = authRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountOverviewResponse getOwnAccountOverview(Long userId) {
        User user = authRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found."));

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<AccountResponse> accountResponses = accounts.stream()
                .map(AccountMapper::toAccountResponse)
                .toList();

        return new AccountOverviewResponse(
                UserMapper.toUserResponse(user),
                accountResponses,
                combinedBalanceOf(accounts)
        );
    }

    private BigDecimal combinedBalanceOf(List<Account> accounts) {
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

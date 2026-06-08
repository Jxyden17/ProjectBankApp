package nl.donniebankoebarkie.api.controller;

import jakarta.validation.Valid;
import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;
import nl.donniebankoebarkie.api.dto.account.AccountResponse;
import nl.donniebankoebarkie.api.dto.account.UpdateAccountRequest;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.IAccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final IAccountService accountService;

    public AccountController(IAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountOverviewResponse getOwnAccounts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return accountService.getOwnAccountOverview(authenticatedUser.userId());
    }

    @GetMapping
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return accountService.listAccounts(authenticatedUser.userId(), authenticatedUser.role());
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return accountService.getAccountById(accountId, authenticatedUser.userId(), authenticatedUser.role());
    }

    @PatchMapping("/{accountId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public AccountResponse updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return accountService.updateAccount(accountId, request);
    }
}

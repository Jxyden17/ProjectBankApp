package nl.donniebankoebarkie.api.controller;

import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.IAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

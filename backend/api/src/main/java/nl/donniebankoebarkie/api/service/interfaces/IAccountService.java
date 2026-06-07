package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;
import nl.donniebankoebarkie.api.dto.account.AccountResponse;
import nl.donniebankoebarkie.api.dto.account.UpdateAccountRequest;
import nl.donniebankoebarkie.api.model.enums.UserRole;

import java.util.List;

public interface IAccountService {
    AccountOverviewResponse getOwnAccountOverview(Long userId);

    List<AccountResponse> listAccounts(Long callerId, UserRole callerRole);

    AccountResponse getAccountById(Long accountId, Long callerId, UserRole callerRole);

    AccountResponse updateAccount(Long accountId, UpdateAccountRequest request);
}

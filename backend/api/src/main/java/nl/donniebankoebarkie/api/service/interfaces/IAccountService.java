package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.account.AccountOverviewResponse;

public interface IAccountService {
    AccountOverviewResponse getOwnAccountOverview(Long userId);
}

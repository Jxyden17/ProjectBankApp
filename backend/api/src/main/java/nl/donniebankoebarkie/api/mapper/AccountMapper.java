package nl.donniebankoebarkie.api.mapper;

import nl.donniebankoebarkie.api.dto.account.AccountResponse;
import nl.donniebankoebarkie.api.model.Account;

public final class AccountMapper {
    private AccountMapper() {
    }

    public static AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getIban(),
                account.getUserId(),
                account.getAccountType(),
                account.getBalance(),
                account.getAbsoluteTransferLimit(),
                account.getDailyTransferLimit(),
                account.isActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

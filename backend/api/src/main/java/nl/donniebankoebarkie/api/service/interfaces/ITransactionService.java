package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.model.enums.TransactionType;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ITransactionService {
    PagedTransactionResponse listTransactions(
            LocalDate startDate, LocalDate endDate,
            BigDecimal amountEq, BigDecimal amountLt, BigDecimal amountGt,
            String iban, Long customerId, TransactionType transactionType,
            int page, int size,
            AuthenticatedUser authenticatedUser);

    TransactionResponse getTransaction(Long transactionId, AuthenticatedUser authenticatedUser);

    TransactionResponse createTransfer(TransferTransactionRequest request, AuthenticatedUser authenticatedUser);

    TransactionResponse createDeposit(DepositTransactionRequest request, AuthenticatedUser authenticatedUser);

    TransactionResponse createWithdrawal(WithdrawalTransactionRequest request, AuthenticatedUser authenticatedUser);
}
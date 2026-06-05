package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ITransactionService {
    PagedTransactionResponse listTransactions(
            LocalDate startDate, LocalDate endDate,
            BigDecimal amountEq, BigDecimal amountLt, BigDecimal amountGt,
            String iban, Long customerId, TransactionType transactionType,
            int page, int size);

    TransactionResponse getTransaction(Long transactionId);

    TransactionResponse createTransfer(TransferTransactionRequest request, Long initiatedByUserId);

    TransactionResponse createDeposit(DepositTransactionRequest request, Long initiatedByUserId);

    TransactionResponse createWithdrawal(WithdrawalTransactionRequest request, Long initiatedByUserId);
}

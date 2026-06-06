package nl.donniebankoebarkie.api.controller;

import jakarta.validation.Valid;
import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.model.enums.TransactionType;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.ITransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final ITransactionService transactionService;

    public TransactionController(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PagedTransactionResponse listTransactions(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) BigDecimal amountEq,
            @RequestParam(required = false) BigDecimal amountLt,
            @RequestParam(required = false) BigDecimal amountGt,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return transactionService.listTransactions(
                startDate, endDate, amountEq, amountLt, amountGt,
                iban, customerId, transactionType, page, size, authenticatedUser);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return transactionService.getTransaction(transactionId, authenticatedUser);
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody TransferTransactionRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TransactionResponse response = transactionService.createTransfer(request, authenticatedUser);
        return createdWithLocation(response.id());
    }

    @PostMapping("/deposits")
    public ResponseEntity<TransactionResponse> createDeposit(
            @Valid @RequestBody DepositTransactionRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TransactionResponse response = transactionService.createDeposit(request, authenticatedUser);
        return createdWithLocation(response.id());
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<TransactionResponse> createWithdrawal(
            @Valid @RequestBody WithdrawalTransactionRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TransactionResponse response = transactionService.createWithdrawal(request, authenticatedUser);
        return createdWithLocation(response.id());
    }

    private ResponseEntity<TransactionResponse> createdWithLocation(Long transactionId) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/transactions/{id}")
                .buildAndExpand(transactionId)
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).build();
    }
}
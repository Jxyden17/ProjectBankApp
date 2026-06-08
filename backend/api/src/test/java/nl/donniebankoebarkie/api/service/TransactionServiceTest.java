package nl.donniebankoebarkie.api.service;

import nl.donniebankoebarkie.api.dto.transaction.request.DepositTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.TransferTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.request.WithdrawalTransactionRequest;
import nl.donniebankoebarkie.api.dto.transaction.response.PagedTransactionResponse;
import nl.donniebankoebarkie.api.dto.transaction.response.TransactionResponse;
import nl.donniebankoebarkie.api.exception.BadRequestException;
import nl.donniebankoebarkie.api.exception.ResourceNotFoundException;
import nl.donniebankoebarkie.api.model.Account;
import nl.donniebankoebarkie.api.model.Transaction;
import nl.donniebankoebarkie.api.model.enums.AccountType;
import nl.donniebankoebarkie.api.model.enums.Channel;
import nl.donniebankoebarkie.api.model.enums.TransactionType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAccountRepository;
import nl.donniebankoebarkie.api.repository.interfaces.ITransactionRepository;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

        @Mock
        private ITransactionRepository transactionRepository;

        @Mock
        private IAccountRepository accountRepository;

        @InjectMocks
        private TransactionService transactionService;

        @Test
        void listTransactionsSanitizesNegativePageToZero() {
                when(accountRepository.findByUserId(1L)).thenReturn(List.of());
                when(transactionRepository.findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                PagedTransactionResponse response = transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, -5, 20,
                                customer(1L));

                assertEquals(0, response.page().page());
        }

        @Test
        void listTransactionsCapsOversizedPageSizeAt100() {
                when(accountRepository.findByUserId(1L)).thenReturn(List.of());
                ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor = ArgumentCaptor
                                .forClass(org.springframework.data.domain.Pageable.class);
                when(transactionRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                                .thenReturn(new PageImpl<>(List.of()));

                transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, 0, 9999,
                                customer(1L));

                assertEquals(100, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void listTransactionsReturnsEmptyItemsWhenCustomerHasNoAccounts() {
                when(accountRepository.findByUserId(1L)).thenReturn(List.of());
                when(transactionRepository.findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of()));

                PagedTransactionResponse response = transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, 0, 20,
                                customer(1L));

                assertEquals(0, response.items().size());
        }

        @Test
        void listTransactionsMapsAllResponseFieldsCorrectly() {
                Account account = account(10L, 1L);
                Transaction t = transaction(99L, account.getId(), null, new BigDecimal("50.00"),
                                TransactionType.WITHDRAWAL);
                t.setDescription("test desc");
                t.setChannel(Channel.ATM);
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));
                when(transactionRepository.findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(t)));

                TransactionResponse item = transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, 0, 20,
                                customer(1L)).items().getFirst();

                assertEquals(99L, item.id());
                assertEquals(new BigDecimal("50.00"), item.amount());
                assertEquals(TransactionType.WITHDRAWAL, item.transactionType());
                assertEquals("EUR", item.currency());
                assertEquals(Channel.ATM, item.channel());
                assertEquals("test desc", item.description());
                assertEquals(10L, item.fromAccountId());
                assertNull(item.toAccountId());
        }

        @Test
        void listTransactionsForCustomerDoesNotQueryByCustomerIdParam() {
                when(accountRepository.findByUserId(1L)).thenReturn(List.of());
                ArgumentCaptor<Specification<Transaction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
                when(transactionRepository.findAll(specCaptor.capture(),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of()));

                transactionService.listTransactions(
                                null, null, null, null, null, null, 999L, null, 0, 20,
                                customer(1L));

                verify(transactionRepository).findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class));
        }

        @Test
        void listTransactionsEmployeeCanCallWithoutAccountScoping() {
                when(transactionRepository.findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of()));

                transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, 0, 20,
                                employee(5L));

                verify(accountRepository, never()).findByUserId(any());
        }

        @Test
        void listTransactionsReturnsCorrectPageMetadata() {
                when(accountRepository.findByUserId(1L)).thenReturn(List.of());
                when(transactionRepository.findAll(any(Specification.class),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 25));

                PagedTransactionResponse response = transactionService.listTransactions(
                                null, null, null, null, null, null, null, null, 2, 10,
                                customer(1L));

                assertEquals(2, response.page().page());
                assertEquals(10, response.page().size());
                assertEquals(25, response.page().totalElements());
                assertEquals(3, response.page().totalPages());
        }

        @Test
        void getTransactionReturnsTransactionForEmployeeRegardlessOfAccount() {
                Transaction t = transaction(5L, 10L, 20L, new BigDecimal("100.00"), TransactionType.TRANSFER);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));

                TransactionResponse response = transactionService.getTransaction(5L, employee(99L));

                assertEquals(5L, response.id());
                verify(accountRepository, never()).findByUserId(any());
        }

        @Test
        void getTransactionReturnsTransactionWhenCustomerOwnsFromAccount() {
                Account account = account(10L, 1L);
                Transaction t = transaction(5L, account.getId(), null, new BigDecimal("75.00"),
                                TransactionType.WITHDRAWAL);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

                TransactionResponse response = transactionService.getTransaction(5L, customer(1L));

                assertEquals(5L, response.id());
        }

        @Test
        void getTransactionReturnsTransactionWhenCustomerOwnsToAccount() {
                Account account = account(20L, 1L);
                Transaction t = transaction(5L, null, account.getId(), new BigDecimal("50.00"),
                                TransactionType.DEPOSIT);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

                TransactionResponse response = transactionService.getTransaction(5L, customer(1L));

                assertEquals(5L, response.id());
        }

        @Test
        void getTransactionReturnsTransactionWhenCustomerOwnsEitherSideOfTransfer() {
                Account ownAccount = account(10L, 1L);
                Account otherAccount = account(20L, 2L);
                // Customer owns the fromAccount side
                Transaction t = transaction(5L, ownAccount.getId(), otherAccount.getId(), new BigDecimal("100.00"),
                                TransactionType.TRANSFER);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(ownAccount));

                TransactionResponse response = transactionService.getTransaction(5L, customer(1L));

                assertEquals(5L, response.id());
        }

        @Test
        void getTransactionThrowsNotFoundWhenCustomerHasNoInvolvedAccount() {
                Account account = account(10L, 1L);
                Transaction t = transaction(5L, 30L, 40L, new BigDecimal("100.00"), TransactionType.TRANSFER);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.getTransaction(5L, customer(1L)));
        }

        @Test
        void getTransactionThrowsNotFoundForUnknownTransactionId() {
                when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.getTransaction(999L, employee(1L)));
        }

        @Test
        void getTransactionThrowsNotFoundNotForbiddenForCustomerPrivacyReason() {
                Account account = account(10L, 1L);
                Transaction t = transaction(5L, 30L, 40L, new BigDecimal("100.00"), TransactionType.TRANSFER);
                when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
                when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.getTransaction(5L, customer(1L)));
        }

        @Test
        void createTransferSavesTransactionWithCorrectFields() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                        Transaction t = inv.getArgument(0);
                        t.setId(99L);
                        return t;
                });

                TransactionResponse response = transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("100.00"), Channel.WEB,
                                                "Test"),
                                customer(1L));

                assertEquals(TransactionType.TRANSFER, response.transactionType());
                assertEquals(new BigDecimal("100.00"), response.amount());
                assertEquals("EUR", response.currency());
                assertEquals(10L, response.fromAccountId());
                assertEquals(20L, response.toAccountId());
                assertEquals(1L, response.initiatedByUserId());
                assertEquals(Channel.WEB, response.channel());
                assertEquals("Test", response.description());

                ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
                verify(accountRepository, times(2)).save(accountCaptor.capture());
                assertEquals(new BigDecimal("400.00"), accountCaptor.getAllValues().get(0).getBalance());
                assertEquals(new BigDecimal("600.00"), accountCaptor.getAllValues().get(1).getBalance());
        }

        @Test
        void createTransferSetsTimestampOnSavedTransaction() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("50.00"), Channel.WEB,
                                                null),
                                customer(1L));

                assertNotNull(captor.getValue().getTimestamp());
        }

        @Test
        void createTransferThrowsBadRequestWhenBothDestinationFieldsAreAbsent() {
                TransferTransactionRequest request = new TransferTransactionRequest(
                                10L, null, null, new BigDecimal("100.00"), Channel.WEB, null);

                assertThrows(BadRequestException.class,
                                () -> transactionService.createTransfer(request, customer(1L)));

                verify(accountRepository, never()).findById(any());
        }

        @Test
        void createTransferThrowsBadRequestWhenBothDestinationFieldsArePresent() {
                TransferTransactionRequest request = new TransferTransactionRequest(
                                10L, 20L, "NL01INHO0000000001", new BigDecimal("100.00"), Channel.WEB, null);

                assertThrows(BadRequestException.class,
                                () -> transactionService.createTransfer(request, customer(1L)));

                verify(accountRepository, never()).findById(any());
        }

        @Test
        void createTransferThrowsNotFoundWhenFromAccountDoesNotExist() {
                when(accountRepository.findById(10L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.createTransfer(
                                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("100.00"),
                                                                Channel.WEB, null),
                                                customer(1L)));
        }

        @Test
        void createTransferThrowsBadRequestWhenFromAccountIsInactive() {
                Account fromAccount = account(10L, 1L);
                fromAccount.setActive(false);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

                assertThrows(BadRequestException.class,
                                () -> transactionService.createTransfer(
                                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("100.00"),
                                                                Channel.WEB, null),
                                                customer(1L)));
        }

        @Test
        void createTransferThrowsAccessDeniedWhenCustomerDoesNotOwnFromAccount() {
                Account fromAccount = account(10L, 99L); // owned by user 99
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

                assertThrows(AccessDeniedException.class,
                                () -> transactionService.createTransfer(
                                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("100.00"),
                                                                Channel.WEB, null),
                                                customer(1L)));
        }

        @Test
        void createTransferThrowsBadRequestWhenToAccountIsInactive() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                toAccount.setActive(false);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

                assertThrows(BadRequestException.class,
                                () -> transactionService.createTransfer(
                                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("100.00"),
                                                                Channel.WEB, null),
                                                customer(1L)));
        }

        @Test
        void createTransferAllowsEmployeeToTransferFromAnyAccount() {
                Account fromAccount = account(10L, 99L); // not owned by employee
                Account toAccount = account(20L, 1L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("50.00"), Channel.WEB,
                                                null),
                                employee(2L));

                verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void createTransferResolvesInternalIbanToAccountId() {
                Account fromAccount = account(10L, 1L);
                Account internalToAccount = account(20L, 2L);
                internalToAccount.setIban("NL99INHO0000000099");
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findAll()).thenReturn(List.of(internalToAccount));
                when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                TransactionResponse response = transactionService.createTransfer(
                                new TransferTransactionRequest(10L, null, "NL99INHO0000000099", new BigDecimal("25.00"),
                                                Channel.WEB, null),
                                customer(1L));

                assertEquals(20L, response.toAccountId());
        }

        @Test
        void createTransferThrowsNotFoundWhenDestinationIbanDoesNotMatchAnAccount() {
                Account fromAccount = account(10L, 1L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findAll()).thenReturn(List.of()); // IBAN not in our system

                assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransfer(
                                new TransferTransactionRequest(10L, null, "NL99EXTR0000000001", new BigDecimal("25.00"),
                                                Channel.WEB, null),
                                customer(1L)));
        }

        @Test
        void createTransferRejectsCrossCustomerTransfersToNonCheckingAccounts() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                toAccount.setAccountType(AccountType.SAVINGS);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

                assertThrows(BadRequestException.class, () -> transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("25.00"), Channel.WEB,
                                                null),
                                customer(1L)));
        }

        @Test
        void createTransferRejectsWhenDailyLimitWouldBeExceeded() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                Transaction existingTransaction = transaction(1L, fromAccount.getId(), toAccount.getId(),
                                new BigDecimal("900.00"), TransactionType.TRANSFER);
                existingTransaction.setTimestamp(LocalDateTime.now());
                when(transactionRepository.findAll()).thenReturn(List.of(existingTransaction));

                assertThrows(BadRequestException.class, () -> transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("200.00"), Channel.WEB,
                                                null),
                                customer(1L)));
        }

        @Test
        void createTransferSetsNullDescriptionWhenNotProvided() {
                Account fromAccount = account(10L, 1L);
                Account toAccount = account(20L, 2L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createTransfer(
                                new TransferTransactionRequest(10L, 20L, null, new BigDecimal("10.00"), Channel.WEB,
                                                null),
                                customer(1L));

                assertNull(captor.getValue().getDescription());
        }

        @Test
        void createDepositSavesTransactionWithCorrectFields() {
                Account toAccount = account(20L, 1L);
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                when(transactionRepository.save(any())).thenAnswer(inv -> {
                        Transaction t = inv.getArgument(0);
                        t.setId(88L);
                        return t;
                });

                TransactionResponse response = transactionService.createDeposit(
                                new DepositTransactionRequest(20L, new BigDecimal("200.00"), Channel.ATM,
                                                "Cash deposit"),
                                employee(5L));

                assertEquals(TransactionType.DEPOSIT, response.transactionType());
                assertEquals(new BigDecimal("200.00"), response.amount());
                assertEquals("EUR", response.currency());
                assertEquals(20L, response.toAccountId());
                assertNull(response.fromAccountId());
                assertEquals(5L, response.initiatedByUserId());
                assertEquals(Channel.ATM, response.channel());
        }

        @Test
        void createDepositSetsTimestampOnSavedTransaction() {
                Account toAccount = account(20L, 1L);
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createDeposit(
                                new DepositTransactionRequest(20L, new BigDecimal("50.00"), Channel.ATM, null),
                                employee(1L));

                assertNotNull(captor.getValue().getTimestamp());
        }

        @Test
        void createDepositThrowsNotFoundWhenToAccountDoesNotExist() {
                when(accountRepository.findById(20L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.createDeposit(
                                                new DepositTransactionRequest(20L, new BigDecimal("50.00"), Channel.ATM,
                                                                null),
                                                employee(1L)));
        }

        @Test
        void createDepositThrowsBadRequestWhenToAccountIsInactive() {
                Account toAccount = account(20L, 1L);
                toAccount.setActive(false);
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

                assertThrows(BadRequestException.class,
                                () -> transactionService.createDeposit(
                                                new DepositTransactionRequest(20L, new BigDecimal("50.00"), Channel.ATM,
                                                                null),
                                                employee(1L)));
        }

        @Test
        void createDepositDoesNotSetFromAccountId() {
                Account toAccount = account(20L, 1L);
                when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createDeposit(
                                new DepositTransactionRequest(20L, new BigDecimal("100.00"), Channel.ATM, null),
                                employee(1L));

                assertNull(captor.getValue().getFromAccountId());
        }

        @Test
        void createWithdrawalSavesTransactionWithCorrectFields() {
                Account fromAccount = account(10L, 1L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(transactionRepository.save(any())).thenAnswer(inv -> {
                        Transaction t = inv.getArgument(0);
                        t.setId(77L);
                        return t;
                });

                TransactionResponse response = transactionService.createWithdrawal(
                                new WithdrawalTransactionRequest(10L, new BigDecimal("75.00"), Channel.ATM,
                                                "ATM withdrawal"),
                                customer(1L));

                assertEquals(TransactionType.WITHDRAWAL, response.transactionType());
                assertEquals(new BigDecimal("75.00"), response.amount());
                assertEquals("EUR", response.currency());
                assertEquals(10L, response.fromAccountId());
                assertNull(response.toAccountId());
                assertEquals(1L, response.initiatedByUserId());
                assertEquals(Channel.ATM, response.channel());
        }

        @Test
        void createWithdrawalSetsTimestampOnSavedTransaction() {
                Account fromAccount = account(10L, 1L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createWithdrawal(
                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"), Channel.ATM, null),
                                customer(1L));

                assertNotNull(captor.getValue().getTimestamp());
        }

        @Test
        void createWithdrawalThrowsAccessDeniedWhenCustomerDoesNotOwnAccount() {
                Account fromAccount = account(10L, 99L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

                assertThrows(AccessDeniedException.class,
                                () -> transactionService.createWithdrawal(
                                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"),
                                                                Channel.ATM, null),
                                                customer(1L)));
        }

        @Test
        void createWithdrawalThrowsNotFoundWhenAccountDoesNotExist() {
                when(accountRepository.findById(10L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> transactionService.createWithdrawal(
                                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"),
                                                                Channel.ATM, null),
                                                customer(1L)));
        }

        @Test
        void createWithdrawalThrowsBadRequestWhenAccountIsInactive() {
                Account fromAccount = account(10L, 1L);
                fromAccount.setActive(false);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

                assertThrows(BadRequestException.class,
                                () -> transactionService.createWithdrawal(
                                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"),
                                                                Channel.ATM, null),
                                                customer(1L)));
        }

        @Test
        void createWithdrawalAllowsEmployeeToWithdrawFromAnyAccount() {
                Account fromAccount = account(10L, 99L); // not owned by employee
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createWithdrawal(
                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"), Channel.ATM, null),
                                employee(2L));

                verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void createWithdrawalDoesNotSetToAccountId() {
                Account fromAccount = account(10L, 1L);
                when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
                when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

                transactionService.createWithdrawal(
                                new WithdrawalTransactionRequest(10L, new BigDecimal("50.00"), Channel.ATM, null),
                                customer(1L));

                assertNull(captor.getValue().getToAccountId());
        }

        private AuthenticatedUser customer(Long userId) {
                return new AuthenticatedUser(userId, "customer@example.com", UserRole.CUSTOMER);
        }

        private AuthenticatedUser employee(Long userId) {
                return new AuthenticatedUser(userId, "employee@example.com", UserRole.EMPLOYEE);
        }

        private Account account(Long id, Long userId) {
                Account account = new Account();
                account.setId(id);
                account.setIban("NL01INHO%010d".formatted(id));
                account.setUserId(userId);
                account.setAccountType(AccountType.CHECKING);
                account.setBalance(new BigDecimal("500.00"));
                account.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
                account.setDailyTransferLimit(new BigDecimal("1000.00"));
                account.setActive(true);
                account.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
                account.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
                return account;
        }

        private Transaction transaction(Long id, Long fromAccountId, Long toAccountId,
                        BigDecimal amount, TransactionType type) {
                Transaction t = new Transaction();
                t.setId(id);
                t.setFromAccountId(fromAccountId);
                t.setToAccountId(toAccountId);
                t.setAmount(amount);
                t.setCurrency("EUR");
                t.setTransactionType(type);
                t.setChannel(Channel.WEB);
                t.setInitiatedByUserId(1L);
                t.setTimestamp(LocalDateTime.of(2026, 1, 1, 12, 0));
                return t;
        }
}

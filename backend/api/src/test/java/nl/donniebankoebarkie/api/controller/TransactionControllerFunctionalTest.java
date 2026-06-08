package nl.donniebankoebarkie.api.controller;

import com.jayway.jsonpath.JsonPath;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.AccountType;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    from_account_id BIGINT NULL,
                    to_account_id BIGINT NULL,
                    amount DECIMAL(19, 2) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    transaction_type VARCHAR(20) NOT NULL,
                    initiated_by_user_id BIGINT NULL,
                    channel VARCHAR(20) NOT NULL,
                    timestamp DATETIME NOT NULL,
                    description VARCHAR(255) NULL
                )
                """);
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    // --- Authentication ---

    @Test
    void listTransactionsRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/transactions"));
    }

    @Test
    void getTransactionRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void createTransferRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/transactions/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(1L, 2L, null, "50.00")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- List transactions: customer scoping ---

    @Test
    void listTransactionsReturnsOnlyOwnTransactionsForCustomer() throws Exception {
        Long customerId = createUser("customer.list@example.com", "910000001", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.list@example.com", "910000002", UserRole.CUSTOMER, true);

        Long customerAccountId = createAccount(customerId, AccountType.CHECKING, "NL01INHO0000000001", new BigDecimal("500.00"));
        Long otherAccountId = createAccount(otherId, AccountType.CHECKING, "NL02INHO0000000002", new BigDecimal("500.00"));

        createTransaction(customerAccountId, null, new BigDecimal("50.00"), "WITHDRAWAL");
        createTransaction(otherAccountId, null, new BigDecimal("100.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.list@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].fromAccountId").value(customerAccountId));
    }

    @Test
    void listTransactionsIgnoresCustomerIdParamForCustomer() throws Exception {
        Long customerId = createUser("customer.param@example.com", "910000003", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.param@example.com", "910000004", UserRole.CUSTOMER, true);

        Long customerAccountId = createAccount(customerId, AccountType.CHECKING, "NL03INHO0000000003", new BigDecimal("500.00"));
        createAccount(otherId, AccountType.CHECKING, "NL04INHO0000000004", new BigDecimal("500.00"));

        createTransaction(customerAccountId, null, new BigDecimal("50.00"), "WITHDRAWAL");

        // Customer passes another user's ID — should still only see their own
        mockMvc.perform(get("/api/transactions")
                        .param("customerId", otherId.toString())
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.param@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].fromAccountId").value(customerAccountId));
    }

    @Test
    void listTransactionsReturnsAllTransactionsForEmployee() throws Exception {
        Long employeeId = createUser("employee.list@example.com", "910000005", UserRole.EMPLOYEE, true);
        Long customer1Id = createUser("cust1.list@example.com", "910000006", UserRole.CUSTOMER, true);
        Long customer2Id = createUser("cust2.list@example.com", "910000007", UserRole.CUSTOMER, true);

        Long account1Id = createAccount(customer1Id, AccountType.CHECKING, "NL05INHO0000000005", new BigDecimal("500.00"));
        Long account2Id = createAccount(customer2Id, AccountType.CHECKING, "NL06INHO0000000006", new BigDecimal("500.00"));

        createTransaction(account1Id, null, new BigDecimal("10.00"), "WITHDRAWAL");
        createTransaction(account2Id, null, new BigDecimal("20.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.list@example.com", UserRole.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void listTransactionsReturnsEmptyPageForCustomerWithNoTransactions() throws Exception {
        Long customerId = createUser("customer.empty@example.com", "910000008", UserRole.CUSTOMER, true);
        createAccount(customerId, AccountType.CHECKING, "NL07INHO0000000007", new BigDecimal("500.00"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.empty@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // --- Get single transaction ---

    @Test
    void getTransactionReturnsTransactionForOwningCustomer() throws Exception {
        Long customerId = createUser("customer.get@example.com", "910000009", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL08INHO0000000008", new BigDecimal("500.00"));
        Long transactionId = createTransaction(accountId, null, new BigDecimal("30.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.get@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.amount").value(30.00))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"));
    }

    @Test
    void getTransactionReturnsNotFoundWhenCustomerDoesNotOwnTransaction() throws Exception {
        Long customerId = createUser("customer.noaccess@example.com", "910000010", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.noaccess@example.com", "910000011", UserRole.CUSTOMER, true);

        createAccount(customerId, AccountType.CHECKING, "NL09INHO0000000009", new BigDecimal("500.00"));
        Long otherAccountId = createAccount(otherId, AccountType.CHECKING, "NL10INHO0000000010", new BigDecimal("500.00"));
        Long transactionId = createTransaction(otherAccountId, null, new BigDecimal("50.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.noaccess@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getTransactionReturnsAnyTransactionForEmployee() throws Exception {
        Long employeeId = createUser("employee.get@example.com", "910000012", UserRole.EMPLOYEE, true);
        Long customerId = createUser("customer.fortransaction@example.com", "910000013", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL11INHO0000000011", new BigDecimal("500.00"));
        Long transactionId = createTransaction(accountId, null, new BigDecimal("40.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.get@example.com", UserRole.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId));
    }

    @Test
    void getTransactionReturnsNotFoundForNonExistentId() throws Exception {
        Long employeeId = createUser("employee.missing@example.com", "910000014", UserRole.EMPLOYEE, true);

        mockMvc.perform(get("/api/transactions/{id}", 999999L)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.missing@example.com", UserRole.EMPLOYEE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Create transfer ---

    @Test
    void createTransferReturnsCreatedAndLocationHeader() throws Exception {
        Long customerId = createUser("customer.transfer@example.com", "910000015", UserRole.CUSTOMER, true);
        Long fromAccountId = createAccount(customerId, AccountType.CHECKING, "NL12INHO0000000012", new BigDecimal("500.00"));
        Long toAccountId = createAccount(customerId, AccountType.SAVINGS, "NL13INHO0000000013", new BigDecimal("0.00"));

        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.transfer@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(fromAccountId, toAccountId, null, "100.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/transactions/")));

        BigDecimal fromBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, fromAccountId);
        BigDecimal toBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, toAccountId);

        assertEquals(0, new BigDecimal("400.00").compareTo(fromBalance));
        assertEquals(0, new BigDecimal("100.00").compareTo(toBalance));
    }

    @Test
    void createTransferRejectsMissingDestination() throws Exception {
        Long customerId = createUser("customer.nodest@example.com", "910000016", UserRole.CUSTOMER, true);
        Long fromAccountId = createAccount(customerId, AccountType.CHECKING, "NL14INHO0000000014", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.nodest@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(fromAccountId, null, null, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTransferRejectsTransferFromAccountOwnedByOtherCustomer() throws Exception {
        Long customerId = createUser("customer.stealing@example.com", "910000017", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.victim@example.com", "910000018", UserRole.CUSTOMER, true);
        Long otherAccountId = createAccount(otherId, AccountType.CHECKING, "NL15INHO0000000015", new BigDecimal("500.00"));
        Long toAccountId = createAccount(customerId, AccountType.SAVINGS, "NL16INHO0000000016", new BigDecimal("0.00"));

        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.stealing@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(otherAccountId, toAccountId, null, "100.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createTransferRejectsInactiveFromAccount() throws Exception {
        Long customerId = createUser("customer.inactive@example.com", "910000019", UserRole.CUSTOMER, true);
        Long fromAccountId = createAccount(customerId, AccountType.CHECKING, "NL17INHO0000000017", new BigDecimal("500.00"));
        Long toAccountId = createAccount(customerId, AccountType.SAVINGS, "NL18INHO0000000018", new BigDecimal("0.00"));
        jdbcTemplate.update("UPDATE accounts SET is_active = false WHERE id = ?", fromAccountId);

        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.inactive@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(fromAccountId, toAccountId, null, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTransferRejectsWhenDailyLimitWouldBeExceeded() throws Exception {
        Long customerId = createUser("customer.limit@example.com", "910000028", UserRole.CUSTOMER, true);
        Long fromAccountId = createAccount(customerId, AccountType.CHECKING, "NL23INHO0000000023", new BigDecimal("500.00"));
        Long toAccountId = createAccount(customerId, AccountType.SAVINGS, "NL24INHO0000000024", new BigDecimal("0.00"));
        jdbcTemplate.update("""
                INSERT INTO transactions (
                    from_account_id, to_account_id, amount, currency,
                    transaction_type, initiated_by_user_id, channel, timestamp
                ) VALUES (?, ?, ?, 'EUR', 'TRANSFER', ?, 'WEB', NOW())
                """, fromAccountId, toAccountId, new BigDecimal("900.00"), customerId);

        mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.limit@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(fromAccountId, toAccountId, null, "200.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        BigDecimal fromBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, fromAccountId);
        BigDecimal toBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, toAccountId);

        assertEquals(0, new BigDecimal("500.00").compareTo(fromBalance));
        assertEquals(0, new BigDecimal("0.00").compareTo(toBalance));
    }

    @Test
    void listTransactionsFiltersCustomerHistoryByIban() throws Exception {
        Long customerId = createUser("customer.iban@example.com", "910000029", UserRole.CUSTOMER, true);
        Long checkingAccountId = createAccount(customerId, AccountType.CHECKING, "NL25INHO0000000025", new BigDecimal("500.00"));
        Long savingsAccountId = createAccount(customerId, AccountType.SAVINGS, "NL26INHO0000000026", new BigDecimal("0.00"));

        createTransaction(checkingAccountId, null, new BigDecimal("50.00"), "WITHDRAWAL");
        createTransaction(savingsAccountId, null, new BigDecimal("75.00"), "WITHDRAWAL");

        mockMvc.perform(get("/api/transactions")
                        .param("iban", "NL25INHO0000000025")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.iban@example.com", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].fromAccountId").value(checkingAccountId));
    }

    // --- Create deposit ---

    @Test
    void createDepositReturnsCreatedAndLocationHeader() throws Exception {
        Long employeeId = createUser("employee.deposit@example.com", "910000020", UserRole.EMPLOYEE, true);
        Long customerId = createUser("customer.deposit@example.com", "910000021", UserRole.CUSTOMER, true);
        Long toAccountId = createAccount(customerId, AccountType.CHECKING, "NL19INHO0000000019", new BigDecimal("0.00"));

        mockMvc.perform(post("/api/transactions/deposits")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.deposit@example.com", UserRole.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositRequest(toAccountId, "200.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/transactions/")));
    }

    @Test
    void createDepositRejectsInactiveToAccount() throws Exception {
        Long employeeId = createUser("employee.depinactive@example.com", "910000022", UserRole.EMPLOYEE, true);
        Long customerId = createUser("customer.depinactive@example.com", "910000023", UserRole.CUSTOMER, true);
        Long toAccountId = createAccount(customerId, AccountType.CHECKING, "NL20INHO0000000020", new BigDecimal("0.00"));
        jdbcTemplate.update("UPDATE accounts SET is_active = false WHERE id = ?", toAccountId);

        mockMvc.perform(post("/api/transactions/deposits")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.depinactive@example.com", UserRole.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositRequest(toAccountId, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- Create withdrawal ---

    @Test
    void createWithdrawalReturnsCreatedAndLocationHeader() throws Exception {
        Long customerId = createUser("customer.withdraw@example.com", "910000024", UserRole.CUSTOMER, true);
        Long fromAccountId = createAccount(customerId, AccountType.CHECKING, "NL21INHO0000000021", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/transactions/withdrawals")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.withdraw@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawalRequest(fromAccountId, "75.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/transactions/")));
    }

    @Test
    void createWithdrawalRejectsWithdrawalFromAccountOwnedByOtherCustomer() throws Exception {
        Long customerId = createUser("customer.notown@example.com", "910000025", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.owner@example.com", "910000026", UserRole.CUSTOMER, true);
        Long otherAccountId = createAccount(otherId, AccountType.CHECKING, "NL22INHO0000000022", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/transactions/withdrawals")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.notown@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawalRequest(otherAccountId, "50.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createWithdrawalRejectsNonExistentAccount() throws Exception {
        Long customerId = createUser("customer.noaccwith@example.com", "910000027", UserRole.CUSTOMER, true);

        mockMvc.perform(post("/api/transactions/withdrawals")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.noaccwith@example.com", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawalRequest(999999L, "50.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Helpers ---

    private Long createUser(String email, String bsnNumber, UserRole role, boolean approved) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO users (
                    first_name, last_name, email, password_hash, phone_number,
                    bsn_number, role, is_approved, approved_by_user_id, approved_at,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "Test", role.name(), email, "unused", "+31600000000",
                bsnNumber, role.name(), approved, null, approved ? now : null, now, now);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private Long createAccount(Long userId, AccountType accountType, String iban, BigDecimal balance) {
        return createAccount(userId, accountType, iban, balance, new BigDecimal("-500.00"), new BigDecimal("1000.00"));
    }

    private Long createAccount(
            Long userId,
            AccountType accountType,
            String iban,
            BigDecimal balance,
            BigDecimal absoluteTransferLimit,
            BigDecimal dailyTransferLimit) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO accounts (
                    iban, user_id, account_type, balance,
                    absolute_transfer_limit, daily_transfer_limit,
                    is_active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                iban, userId, accountType.name(), balance,
                absoluteTransferLimit, dailyTransferLimit,
                true, now, now);
        return jdbcTemplate.queryForObject("SELECT id FROM accounts WHERE iban = ?", Long.class, iban);
    }

    private Long createTransaction(Long fromAccountId, Long toAccountId, BigDecimal amount, String type) {
        jdbcTemplate.update("""
                INSERT INTO transactions (
                    from_account_id, to_account_id, amount, currency,
                    transaction_type, channel, initiated_by_user_id, timestamp
                ) VALUES (?, ?, ?, 'EUR', ?, 'WEB', 1, NOW())
                """,
                fromAccountId, toAccountId, amount, type);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM transactions ORDER BY id DESC LIMIT 1", Long.class);
    }

    private String tokenFor(Long userId, String email, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(userId, "Test", role.name(), email, "unused",
                "+31600000000", "000000000", role, true, null, now, now, now);
        return jwtService.generateAccessToken(user);
    }

    private String transferRequest(Long fromAccountId, Long toAccountId, String destinationIban, String amount) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"fromAccountId\": ").append(fromAccountId).append(",");
        if (toAccountId != null) {
            json.append("\"toAccountId\": ").append(toAccountId).append(",");
        }
        if (destinationIban != null) {
            json.append("\"destinationIban\": \"").append(destinationIban).append("\",");
        }
        json.append("\"amount\": ").append(amount).append(",");
        json.append("\"channel\": \"WEB\"");
        json.append("}");
        return json.toString();
    }

    private String depositRequest(Long toAccountId, String amount) {
        return """
                {
                  "toAccountId": %d,
                  "amount": %s,
                  "channel": "ATM"
                }
                """.formatted(toAccountId, amount);
    }

    private String withdrawalRequest(Long fromAccountId, String amount) {
        return """
                {
                  "fromAccountId": %d,
                  "amount": %s,
                  "channel": "ATM"
                }
                """.formatted(fromAccountId, amount);
    }
}

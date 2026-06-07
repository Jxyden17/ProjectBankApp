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

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerFunctionalTest {

    private static final String IBAN_PATTERN = "^NL\\d{2}INHO\\d{10}$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void getOwnAccountsRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/accounts/me"));
    }

    @Test
    void getOwnAccountsReturnsAccountsAndCombinedBalanceForAuthenticatedUser() throws Exception {
        Long customerId = createUser("owner.accounts@example.com", "900100001", UserRole.CUSTOMER, true);
        createAccount(customerId, AccountType.CHECKING, "NL01INHO0000000001", new BigDecimal("150.00"));
        createAccount(customerId, AccountType.SAVINGS, "NL02INHO0000000002", new BigDecimal("25.50"));

        MvcResult result = mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + tokenFor(customerId, "owner.accounts@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(customerId))
                .andExpect(jsonPath("$.customer.email").value("owner.accounts@example.com"))
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].iban").value(matchesPattern(IBAN_PATTERN)))
                .andExpect(jsonPath("$.accounts[1].iban").value(matchesPattern(IBAN_PATTERN)))
                .andReturn();

        assertEquals(175.50, combinedBalanceOf(result), 0.001);
    }

    @Test
    void getOwnAccountsReturnsOnlyTheAuthenticatedUsersAccounts() throws Exception {
        Long callerId = createUser("caller.accounts@example.com", "900100002", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.accounts@example.com", "900100003", UserRole.CUSTOMER, true);
        createAccount(callerId, AccountType.CHECKING, "NL03INHO0000000003", new BigDecimal("100.00"));
        createAccount(otherId, AccountType.CHECKING, "NL04INHO0000000004", new BigDecimal("999.00"));

        MvcResult result = mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + tokenFor(callerId, "caller.accounts@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].userId").value(callerId))
                .andReturn();

        assertEquals(100.00, combinedBalanceOf(result), 0.001);
    }

    @Test
    void getOwnAccountsReturnsEmptyOverviewForUserWithoutAccounts() throws Exception {
        Long employeeId = createUser("employee.accounts@example.com", "900100004", UserRole.EMPLOYEE, true);

        MvcResult result = mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.accounts@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(employeeId))
                .andExpect(jsonPath("$.accounts.length()").value(0))
                .andReturn();

        assertEquals(0.0, combinedBalanceOf(result), 0.001);
    }

    @Test
    void listAccountsRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/accounts"));
    }

    @Test
    void listAccountsLetsEmployeesSeeEveryAccount() throws Exception {
        Long employeeId = createUser("employee.list@example.com", "900200001", UserRole.EMPLOYEE, true);
        Long firstCustomerId = createUser("first.list@example.com", "900200002", UserRole.CUSTOMER, true);
        Long secondCustomerId = createUser("second.list@example.com", "900200003", UserRole.CUSTOMER, true);
        createAccount(firstCustomerId, AccountType.CHECKING, "NL10INHO0000000010", new BigDecimal("100.00"));
        createAccount(firstCustomerId, AccountType.SAVINGS, "NL11INHO0000000011", new BigDecimal("200.00"));
        createAccount(secondCustomerId, AccountType.CHECKING, "NL12INHO0000000012", new BigDecimal("300.00"));

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.list@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void listAccountsReturnsOnlyTheCustomersOwnAccounts() throws Exception {
        Long callerId = createUser("caller.scope@example.com", "900200009", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.scope@example.com", "900200010", UserRole.CUSTOMER, true);
        createAccount(callerId, AccountType.CHECKING, "NL17INHO0000000017", new BigDecimal("100.00"));
        createAccount(otherId, AccountType.CHECKING, "NL18INHO0000000018", new BigDecimal("999.00"));

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + tokenFor(callerId, "caller.scope@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(callerId));
    }

    @Test
    void getAccountByIdRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/accounts/1"));
    }

    @Test
    void getAccountByIdLetsEmployeesViewAnyAccount() throws Exception {
        Long employeeId = createUser("employee.details@example.com", "900300001", UserRole.EMPLOYEE, true);
        Long customerId = createUser("customer.details@example.com", "900300002", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL20INHO0000000020", new BigDecimal("150.00"));

        mockMvc.perform(get("/api/accounts/{accountId}", accountId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.details@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.userId").value(customerId))
                .andExpect(jsonPath("$.iban").value("NL20INHO0000000020"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"));
    }

    @Test
    void getAccountByIdLetsCustomersViewTheirOwnAccount() throws Exception {
        Long customerId = createUser("owner.details@example.com", "900300003", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.SAVINGS, "NL21INHO0000000021", new BigDecimal("75.00"));

        mockMvc.perform(get("/api/accounts/{accountId}", accountId)
                        .header("Authorization", "Bearer " + tokenFor(customerId, "owner.details@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.userId").value(customerId));
    }

    @Test
    void getAccountByIdForbidsCustomersFromViewingAnotherUsersAccount() throws Exception {
        Long callerId = createUser("caller.details@example.com", "900300004", UserRole.CUSTOMER, true);
        Long otherId = createUser("other.details@example.com", "900300005", UserRole.CUSTOMER, true);
        Long otherAccountId = createAccount(otherId, AccountType.CHECKING, "NL22INHO0000000022", new BigDecimal("999.00"));

        mockMvc.perform(get("/api/accounts/{accountId}", otherAccountId)
                        .header("Authorization", "Bearer " + tokenFor(callerId, "caller.details@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAccountByIdReturnsNotFoundForUnknownAccount() throws Exception {
        Long employeeId = createUser("employee.missing@example.com", "900300006", UserRole.EMPLOYEE, true);

        mockMvc.perform(get("/api/accounts/{accountId}", 999999)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.missing@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateAccountRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(patch("/api/accounts/{accountId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void updateAccountRejectsCustomerBearerToken() throws Exception {
        Long customerId = createUser("customer.patch@example.com", "900400001", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL30INHO0000000030", new BigDecimal("100.00"));

        mockMvc.perform(patch("/api/accounts/{accountId}", accountId)
                        .header("Authorization", "Bearer " + tokenFor(customerId, "customer.patch@example.com", UserRole.CUSTOMER, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateAccountLetsEmployeesUpdateLimitsAndStatus() throws Exception {
        Long employeeId = createUser("employee.patch@example.com", "900400002", UserRole.EMPLOYEE, true);
        Long customerId = createUser("target.patch@example.com", "900400003", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL31INHO0000000031", new BigDecimal("100.00"));

        mockMvc.perform(patch("/api/accounts/{accountId}", accountId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.patch@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "absoluteTransferLimit": -250.00,
                                  "dailyTransferLimit": 1500.00,
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.isActive").value(false));

        BigDecimal dailyLimit = jdbcTemplate.queryForObject(
                "SELECT daily_transfer_limit FROM accounts WHERE id = ?", BigDecimal.class, accountId);
        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM accounts WHERE id = ?", Boolean.class, accountId);

        assertEquals(0, new BigDecimal("1500.00").compareTo(dailyLimit));
        assertEquals(false, active);
    }

    @Test
    void updateAccountRejectsNegativeDailyLimit() throws Exception {
        Long employeeId = createUser("employee.patchbad@example.com", "900400004", UserRole.EMPLOYEE, true);
        Long customerId = createUser("target.patchbad@example.com", "900400005", UserRole.CUSTOMER, true);
        Long accountId = createAccount(customerId, AccountType.CHECKING, "NL32INHO0000000032", new BigDecimal("100.00"));

        mockMvc.perform(patch("/api/accounts/{accountId}", accountId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.patchbad@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyTransferLimit": -1.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateAccountReturnsNotFoundForUnknownAccount() throws Exception {
        Long employeeId = createUser("employee.patchmissing@example.com", "900400006", UserRole.EMPLOYEE, true);

        mockMvc.perform(patch("/api/accounts/{accountId}", 999999)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.patchmissing@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String updateBody() {
        return """
                {
                  "absoluteTransferLimit": -250.00,
                  "dailyTransferLimit": 1500.00,
                  "isActive": false
                }
                """;
    }

    private double combinedBalanceOf(MvcResult result) throws Exception {
        Number combinedBalance = JsonPath.read(result.getResponse().getContentAsString(), "$.combinedBalance");
        return combinedBalance.doubleValue();
    }

    private Long createAccount(Long userId, AccountType accountType, String iban, BigDecimal balance) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO accounts (
                    iban,
                    user_id,
                    account_type,
                    balance,
                    absolute_transfer_limit,
                    daily_transfer_limit,
                    is_active,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                iban,
                userId,
                accountType.name(),
                balance,
                new BigDecimal("-500.00"),
                new BigDecimal("1000.00"),
                true,
                now,
                now
        );

        return jdbcTemplate.queryForObject("SELECT id FROM accounts WHERE iban = ?", Long.class, iban);
    }

    private Long createUser(String email, String bsnNumber, UserRole role, boolean approved) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO users (
                    first_name,
                    last_name,
                    email,
                    password_hash,
                    phone_number,
                    bsn_number,
                    role,
                    is_approved,
                    approved_by_user_id,
                    approved_at,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "Test",
                role.name(),
                email,
                "unused",
                "+31600000000",
                bsnNumber,
                role.name(),
                approved,
                null,
                approved ? now : null,
                now,
                now
        );

        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private String tokenFor(Long userId, String email, UserRole role, boolean approved) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                userId,
                "Test",
                role.name(),
                email,
                "unused",
                "+31600000000",
                "000000000",
                role,
                approved,
                null,
                approved ? now : null,
                now,
                now
        );
        return jwtService.generateAccessToken(user);
    }
}

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private double combinedBalanceOf(MvcResult result) throws Exception {
        Number combinedBalance = JsonPath.read(result.getResponse().getContentAsString(), "$.combinedBalance");
        return combinedBalance.doubleValue();
    }

    private void createAccount(Long userId, AccountType accountType, String iban, BigDecimal balance) {
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

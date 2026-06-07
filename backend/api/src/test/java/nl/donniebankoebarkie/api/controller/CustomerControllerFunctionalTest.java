package nl.donniebankoebarkie.api.controller;

import com.jayway.jsonpath.JsonPath;
import nl.donniebankoebarkie.api.model.User;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerFunctionalTest {

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
    void listPendingCustomersRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/customers/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/customers/pending"));
    }

    @Test
    void listPendingCustomersRejectsCustomerBearerToken() throws Exception {
        Long callerCustomerId = createUser("caller.pending@example.com", "900000008", UserRole.CUSTOMER, true);

        mockMvc.perform(get("/api/customers/pending")
                        .header("Authorization", "Bearer " + tokenFor(callerCustomerId, "caller.pending@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listPendingCustomersReturnsOnlyUnapprovedCustomersForEmployees() throws Exception {
        Long employeeId = createUser("employee.pending@example.com", "900000009", UserRole.EMPLOYEE, true);
        Long pendingCustomerId = createUser("pending.visible@example.com", "900000010", UserRole.CUSTOMER, false);
        createUser("approved.hidden@example.com", "900000011", UserRole.CUSTOMER, true);

        mockMvc.perform(get("/api/customers/pending")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.pending@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(pendingCustomerId))
                .andExpect(jsonPath("$.items[0].email").value("pending.visible@example.com"))
                .andExpect(jsonPath("$.items[0].approved").value(false))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void listCustomersRejectsCustomerBearerToken() throws Exception {
        Long callerCustomerId = createUser("caller.list@example.com", "900000016", UserRole.CUSTOMER, true);

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenFor(callerCustomerId, "caller.list@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listCustomersFiltersCustomersForEmployees() throws Exception {
        Long employeeId = createUser("employee.list@example.com", "900000017", UserRole.EMPLOYEE, true);
        Long matchingCustomerId = createUser("alice.approved@example.com", "900000018", UserRole.CUSTOMER, true, "Alice", "Zeeland");
        createUser("alice.pending@example.com", "900000019", UserRole.CUSTOMER, false, "Alice", "Pending");
        createUser("bob.approved@example.com", "900000020", UserRole.CUSTOMER, true, "Bob", "Zeeland");
        createUser("employee.hidden@example.com", "900000021", UserRole.EMPLOYEE, true, "Alice", "Zeeland");

        mockMvc.perform(get("/api/customers")
                        .queryParam("approved", "true")
                        .queryParam("firstName", "ali")
                        .queryParam("lastName", "zee")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort", "email,asc")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.list@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matchingCustomerId))
                .andExpect(jsonPath("$.items[0].email").value("alice.approved@example.com"))
                .andExpect(jsonPath("$.items[0].approved").value(true))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getCustomerReturnsCustomerDetailsForEmployees() throws Exception {
        Long employeeId = createUser("employee.detail@example.com", "900000022", UserRole.EMPLOYEE, true);
        Long customerId = createUser("detail.customer@example.com", "900000023", UserRole.CUSTOMER, true, "Detail", "Customer");

        mockMvc.perform(get("/api/customers/{customerId}", customerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.detail@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.firstName").value("Detail"))
                .andExpect(jsonPath("$.lastName").value("Customer"))
                .andExpect(jsonPath("$.email").value("detail.customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void getCustomerRejectsEmployeeTargetsAsNotFound() throws Exception {
        Long employeeId = createUser("employee.detail-target@example.com", "900000024", UserRole.EMPLOYEE, true);
        Long targetEmployeeId = createUser("employee.not-customer@example.com", "900000025", UserRole.EMPLOYEE, true);

        mockMvc.perform(get("/api/customers/{customerId}", targetEmployeeId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.detail-target@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void lookupCustomerIbansRequiresFirstNameAndLastName() throws Exception {
        Long callerCustomerId = createUser("caller.lookup-required@example.com", "900000026", UserRole.CUSTOMER, true);

        mockMvc.perform(get("/api/customers/lookup")
                        .queryParam("firstName", "Jane")
                        .header("Authorization", "Bearer " + tokenFor(callerCustomerId, "caller.lookup-required@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void lookupCustomerIbansRejectsEmployeeBearerToken() throws Exception {
        Long employeeId = createUser("employee.lookup@example.com", "900000027", UserRole.EMPLOYEE, true);

        mockMvc.perform(get("/api/customers/lookup")
                        .queryParam("firstName", "Jane")
                        .queryParam("lastName", "Smith")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.lookup@example.com", UserRole.EMPLOYEE, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void lookupCustomerIbansReturnsMatchingActiveCustomerAccounts() throws Exception {
        Long callerCustomerId = createUser("caller.lookup@example.com", "900000028", UserRole.CUSTOMER, true);
        Long matchingCustomerId = createUser("jane.smith@example.com", "900000029", UserRole.CUSTOMER, true, "Jane", "Smith");
        Long hiddenCustomerId = createUser("jane.hidden@example.com", "900000030", UserRole.CUSTOMER, true, "Jane", "Hidden");
        createAccount(matchingCustomerId, "NL12INHO0001234567", true);
        createAccount(hiddenCustomerId, "NL99INHO0009999999", true);
        createAccount(matchingCustomerId, "NL34INHO0007654321", false);

        mockMvc.perform(get("/api/customers/lookup")
                        .queryParam("firstName", "Jane")
                        .queryParam("lastName", "Smith")
                        .queryParam("size", "10")
                        .header("Authorization", "Bearer " + tokenFor(callerCustomerId, "caller.lookup@example.com", UserRole.CUSTOMER, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].firstName").value("Jane"))
                .andExpect(jsonPath("$.items[0].lastName").value("Smith"))
                .andExpect(jsonPath("$.items[0].iban").value("NL12INHO0001234567"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void approveCustomerRejectsRequestsWithoutBearerToken() throws Exception {
        Long customerId = createUser("pending.unauthorized@example.com", "900000001", UserRole.CUSTOMER, false);

        mockMvc.perform(patch("/api/customers/{customerId}/approval", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/customers/%d/approval".formatted(customerId)));
    }

    @Test
    void approveCustomerRejectsCustomerBearerToken() throws Exception {
        Long pendingCustomerId = createUser("pending.forbidden@example.com", "900000002", UserRole.CUSTOMER, false);
        Long callerCustomerId = createUser("caller.customer@example.com", "900000003", UserRole.CUSTOMER, true);

        mockMvc.perform(patch("/api/customers/{customerId}/approval", pendingCustomerId)
                        .header("Authorization", "Bearer " + tokenFor(callerCustomerId, "caller.customer@example.com", UserRole.CUSTOMER, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void approveCustomerCreatesCheckingAndSavingsAccountsWithUniqueIbans() throws Exception {
        Long employeeId = createUser("employee.approver@example.com", "900000004", UserRole.EMPLOYEE, true);
        Long customerId = createUser("pending.approval@example.com", "900000005", UserRole.CUSTOMER, false);

        MvcResult result = mockMvc.perform(patch("/api/customers/{customerId}/approval", customerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.approver@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(customerId))
                .andExpect(jsonPath("$.customer.approved").value(true))
                .andExpect(jsonPath("$.customer.approvedByUserId").value(employeeId))
                .andExpect(jsonPath("$.customer.approvedAt").exists())
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].iban").value(matchesPattern("^NL\\d{2}INHO\\d{10}$")))
                .andExpect(jsonPath("$.accounts[1].iban").value(matchesPattern("^NL\\d{2}INHO\\d{10}$")))
                .andReturn();

        List<String> accountTypes = jdbcTemplate.queryForList(
                "SELECT account_type FROM accounts WHERE user_id = ? ORDER BY account_type",
                String.class,
                customerId
        );
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE user_id = ?",
                Integer.class,
                customerId
        );
        Boolean approved = jdbcTemplate.queryForObject(
                "SELECT is_approved FROM users WHERE id = ?",
                Boolean.class,
                customerId
        );

        String firstIban = JsonPath.read(result.getResponse().getContentAsString(), "$.accounts[0].iban");
        String secondIban = JsonPath.read(result.getResponse().getContentAsString(), "$.accounts[1].iban");

        assertEquals(List.of("CHECKING", "SAVINGS"), accountTypes);
        assertEquals(2, accountCount);
        assertEquals(true, approved);
        assertNotEquals(firstIban, secondIban);
    }

    @Test
    void approveCustomerRollsBackApprovalWhenAccountCreationFails() {
        Long employeeId = createUser("employee.rollback@example.com", "900000014", UserRole.EMPLOYEE, true);
        Long customerId = createUser("pending.rollback@example.com", "900000015", UserRole.CUSTOMER, false);

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(patch("/api/customers/{customerId}/approval", customerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.rollback@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalRequestWithOverflowingAccountLimit()))
        );

        Boolean approved = jdbcTemplate.queryForObject(
                "SELECT is_approved FROM users WHERE id = ?",
                Boolean.class,
                customerId
        );
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE user_id = ?",
                Integer.class,
                customerId
        );

        assertFalse(approved);
        assertEquals(0, accountCount);
    }

    @Test
    void approveCustomerRejectsAlreadyApprovedCustomer() throws Exception {
        Long employeeId = createUser("employee.conflict@example.com", "900000006", UserRole.EMPLOYEE, true);
        Long customerId = createUser("already.approved@example.com", "900000007", UserRole.CUSTOMER, true);

        mockMvc.perform(patch("/api/customers/{customerId}/approval", customerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.conflict@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void approveCustomerRejectsNegativeDailyLimit() throws Exception {
        Long employeeId = createUser("employee.validation@example.com", "900000012", UserRole.EMPLOYEE, true);
        Long customerId = createUser("pending.validation@example.com", "900000013", UserRole.CUSTOMER, false);

        mockMvc.perform(patch("/api/customers/{customerId}/approval", customerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, "employee.validation@example.com", UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "checkingAbsoluteTransferLimit": -500.00,
                                  "checkingDailyTransferLimit": -1.00,
                                  "savingsAbsoluteTransferLimit": 0.00,
                                  "savingsDailyTransferLimit": 5000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private Long createUser(String email, String bsnNumber, UserRole role, boolean approved) {
        return createUser(email, bsnNumber, role, approved, "Test", role.name());
    }

    private Long createUser(String email, String bsnNumber, UserRole role, boolean approved, String firstName, String lastName) {
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
                firstName,
                lastName,
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

    private void createAccount(Long userId, String iban, boolean active) {
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
                "CHECKING",
                0,
                -500,
                1000,
                active,
                now,
                now
        );
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

    private String approvalRequest() {
        return """
                {
                  "approved": true,
                  "checkingAbsoluteTransferLimit": -500.00,
                  "checkingDailyTransferLimit": 1000.00,
                  "savingsAbsoluteTransferLimit": 0.00,
                  "savingsDailyTransferLimit": 5000.00
                }
                """;
    }

    private String approvalRequestWithOverflowingAccountLimit() {
        return """
                {
                  "approved": true,
                  "checkingAbsoluteTransferLimit": 999999999999999999999999999.00,
                  "checkingDailyTransferLimit": 1000.00,
                  "savingsAbsoluteTransferLimit": 0.00,
                  "savingsDailyTransferLimit": 5000.00
                }
                """;
    }
}

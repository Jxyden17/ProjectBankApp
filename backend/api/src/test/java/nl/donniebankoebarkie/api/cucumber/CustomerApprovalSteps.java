package nl.donniebankoebarkie.api.cucumber;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

public class CustomerApprovalSteps {
    private static final String DEFAULT_EMPLOYEE_EMAIL = "employee.cucumber@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private Long employeeId;
    private Long customerId;
    private MvcResult lastResult;

    @Before
    public void resetDatabase() {
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
        employeeId = null;
        customerId = null;
        lastResult = null;
    }

    @Given("an employee exists")
    public void anEmployeeExists() {
        employeeId = createUser(DEFAULT_EMPLOYEE_EMAIL, "800000001", UserRole.EMPLOYEE, true);
    }

    @Given("a pending customer {string} exists")
    public void aPendingCustomerExists(String email) {
        customerId = createUser(email, nextBsn(), UserRole.CUSTOMER, false);
    }

    @Given("an approved customer {string} exists")
    public void anApprovedCustomerExists(String email) {
        createUser(email, nextBsn(), UserRole.CUSTOMER, true);
    }

    @When("the employee requests pending customers")
    public void theEmployeeRequestsPendingCustomers() throws Exception {
        lastResult = mockMvc.perform(get("/api/customers/pending")
                        .header("Authorization", "Bearer " + tokenFor(employeeId, DEFAULT_EMPLOYEE_EMAIL, UserRole.EMPLOYEE, true)))
                .andReturn();
    }

    @When("the customer requests pending customers")
    public void theCustomerRequestsPendingCustomers() throws Exception {
        String email = emailForUser(customerId);
        lastResult = mockMvc.perform(get("/api/customers/pending")
                        .header("Authorization", "Bearer " + tokenFor(customerId, email, UserRole.CUSTOMER, false)))
                .andReturn();
    }

    @When("the employee approves {string} with default account limits")
    public void theEmployeeApprovesWithDefaultAccountLimits(String email) throws Exception {
        Long targetCustomerId = idForEmail(email);

        lastResult = mockMvc.perform(patch("/api/customers/{customerId}/approval", targetCustomerId)
                        .header("Authorization", "Bearer " + tokenFor(employeeId, DEFAULT_EMPLOYEE_EMAIL, UserRole.EMPLOYEE, true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "checkingAbsoluteTransferLimit": -500.00,
                                  "checkingDailyTransferLimit": 1000.00,
                                  "savingsAbsoluteTransferLimit": 0.00,
                                  "savingsDailyTransferLimit": 5000.00
                                }
                                """))
                .andReturn();
    }

    @Then("the pending customer list contains {string}")
    public void thePendingCustomerListContains(String email) throws Exception {
        assertEquals(200, lastResult.getResponse().getStatus());
        List<String> emails = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.items[*].email");
        assertTrue(emails.contains(email));
    }

    @Then("the pending customer list does not contain {string}")
    public void thePendingCustomerListDoesNotContain(String email) throws Exception {
        List<String> emails = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.items[*].email");
        assertFalse(emails.contains(email));
    }

    @Then("the approval response contains {int} accounts")
    public void theApprovalResponseContainsAccounts(int expectedAccountCount) throws Exception {
        assertEquals(200, lastResult.getResponse().getStatus());
        List<Object> accounts = JsonPath.read(lastResult.getResponse().getContentAsString(), "$.accounts[*]");
        assertEquals(expectedAccountCount, accounts.size());
    }

    @Then("the customer {string} is approved")
    public void theCustomerIsApproved(String email) {
        Boolean approved = jdbcTemplate.queryForObject(
                "SELECT is_approved FROM users WHERE email = ?",
                Boolean.class,
                email
        );
        assertEquals(true, approved);
    }

    @Then("the customer {string} has checking and savings accounts")
    public void theCustomerHasCheckingAndSavingsAccounts(String email) {
        Long userId = idForEmail(email);
        List<String> accountTypes = jdbcTemplate.queryForList(
                "SELECT account_type FROM accounts WHERE user_id = ? ORDER BY account_type",
                String.class,
                userId
        );

        assertEquals(List.of("CHECKING", "SAVINGS"), accountTypes);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertEquals(expectedStatus, lastResult.getResponse().getStatus());
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
                "Cucumber",
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

        return idForEmail(email);
    }

    private String tokenFor(Long userId, String email, UserRole role, boolean approved) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                userId,
                "Cucumber",
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

    private Long idForEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private String emailForUser(Long userId) {
        return jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId);
    }

    private String nextBsn() {
        Long nextValue = jdbcTemplate.queryForObject("SELECT COUNT(*) + 800000100 FROM users", Long.class);
        return String.valueOf(nextValue);
    }
}

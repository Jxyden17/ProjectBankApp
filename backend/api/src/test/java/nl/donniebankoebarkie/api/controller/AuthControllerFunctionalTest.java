package nl.donniebankoebarkie.api.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerFunctionalTest {
    private static final String STRONG_PASSWORD = "Welkom123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerCreatesCustomerAsUnapprovedUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Charlie",
                                  "lastName": "Student",
                                  "email": "charlie.student@example.com",
                                  "password": "%s",
                                  "phoneNumber": "+31611112222",
                                  "bsnNumber": "111222333"
                                }
                                """.formatted(STRONG_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/")))
                .andExpect(jsonPath("$.email").value("charlie.student@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void registerDoesNotCreateAccountsForNewCustomer() throws Exception {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL
                )
                """);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Charlie",
                                  "lastName": "Student",
                                  "email": "charlie.accounts@example.com",
                                  "password": "%s",
                                  "phoneNumber": "+31611112222",
                                  "bsnNumber": "222333444"
                                }
                                """.formatted(STRONG_PASSWORD)))
                .andExpect(status().isCreated());

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "charlie.accounts@example.com"
        );
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE user_id = ?",
                Integer.class,
                userId
        );

        org.junit.jupiter.api.Assertions.assertEquals(0, accountCount);
    }

    @Test
    void registerRejectsWeakPasswordAtRequestBoundaryWithValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Charlie",
                                  "lastName": "Student",
                                  "email": "charlie.student@example.com",
                                  "password": "welcome",
                                  "phoneNumber": "+31611112222",
                                  "bsnNumber": "111222333"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "password must be at least 8 characters and include uppercase, lowercase, and a digit."))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void loginReturnsAccessTokenAndRefreshCookieForSeededUser() throws Exception {
        registerCustomer("emma.customer@projectbank.local", "123456789");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("emma.customer@projectbank.local")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("emma.customer@projectbank.local"));
    }

    @Test
    void refreshRotatesCookieAndReturnsNewAccessToken() throws Exception {
        registerCustomer("emma.refresh@projectbank.local", "123456780");

        MvcResult loginResult = loginCustomer("emma.refresh@projectbank.local");

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginThenCurrentUserReturnsProfileForBearerToken() throws Exception {
        registerCustomer("emma.profile@projectbank.local", "123456781");

        MvcResult loginResult = loginCustomer("emma.profile@projectbank.local");

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        Number userId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.user.id");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.longValue()))
                .andExpect(jsonPath("$.email").value("emma.profile@projectbank.local"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void logoutClearsRefreshCookie() throws Exception {
        registerCustomer("emma.logout@projectbank.local", "123456782");

        MvcResult loginResult = loginCustomer("emma.logout@projectbank.local");

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(header().string("Set-Cookie", notNullValue()));
    }

    private void registerCustomer(String email, String bsnNumber) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(email, bsnNumber)))
                .andExpect(status().isCreated());
    }

    private MvcResult loginCustomer(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String registerRequest(String email, String bsnNumber) {
        return """
                {
                  "firstName": "Emma",
                  "lastName": "Customer",
                  "email": "%s",
                  "password": "%s",
                  "phoneNumber": "+31612345678",
                  "bsnNumber": "%s"
                }
                """.formatted(email, STRONG_PASSWORD, bsnNumber);
    }

    private String loginRequest(String email) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, STRONG_PASSWORD);
    }
}

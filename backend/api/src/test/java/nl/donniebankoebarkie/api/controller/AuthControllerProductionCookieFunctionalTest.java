package nl.donniebankoebarkie.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("prod")
class AuthControllerProductionCookieFunctionalTest {
    private static final String STRONG_PASSWORD = "Welkom123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsSecureRefreshCookieWhenProdProfileIsActive() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Paula",
                                  "lastName": "Prod",
                                  "email": "paula.prod@example.com",
                                  "password": "%s",
                                  "phoneNumber": "+31611112222",
                                  "bsnNumber": "555666777"
                                }
                                """.formatted(STRONG_PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "paula.prod@example.com",
                                  "password": "%s"
                                }
                                """.formatted(STRONG_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }
}

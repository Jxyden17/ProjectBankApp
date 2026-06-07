package nl.donniebankoebarkie.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

    private final HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
    private final XorCsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
            new XorCsrfTokenRequestAttributeHandler();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsSecureRefreshCookieWhenProdProfileIsActive() throws Exception {
        mockMvc.perform(withCsrf(post("/api/auth/register")
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
                                """.formatted(STRONG_PASSWORD))))
                .andExpect(status().isCreated());

        mockMvc.perform(withCsrf(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "paula.prod@example.com",
                                  "password": "%s"
                                }
                                """.formatted(STRONG_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder requestBuilder) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        csrfTokenRequestHandler.handle(request, response, () -> csrfToken);
        CsrfToken maskedCsrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        return requestBuilder
                .session((MockHttpSession) request.getSession())
                .param(maskedCsrfToken.getParameterName(), maskedCsrfToken.getToken());
    }
}

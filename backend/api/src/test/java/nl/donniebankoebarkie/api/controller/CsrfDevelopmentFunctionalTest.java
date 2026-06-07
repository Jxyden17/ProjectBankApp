package nl.donniebankoebarkie.api.controller;

import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import nl.donniebankoebarkie.api.repository.interfaces.IAuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:h2:mem:csrfdev;MODE=MariaDB;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Transactional
class CsrfDevelopmentFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IAuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginAcceptsPostWithoutCsrfTokenInDevelopment() throws Exception {
        createApprovedCustomer();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "csrf.development@example.com",
                                  "password": "Welkom123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private void createApprovedCustomer() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setFirstName("Csrf");
        user.setLastName("Development");
        user.setEmail("csrf.development@example.com");
        user.setPasswordHash(passwordEncoder.encode("Welkom123"));
        user.setPhoneNumber("+31600000000");
        user.setBsnNumber("900000031");
        user.setRole(UserRole.CUSTOMER);
        user.setApproved(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        authRepository.save(user);
    }
}

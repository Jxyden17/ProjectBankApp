package nl.donniebankoebarkie.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void passwordEncoderUsesOwapsMinimumArgon2Parameters() {
        PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

        String encodedPassword = passwordEncoder.encode("welkom123");

        assertTrue(encodedPassword.contains("m=19456,t=2,p=1"));
        assertTrue(passwordEncoder.matches("welkom123", encodedPassword));
    }
}

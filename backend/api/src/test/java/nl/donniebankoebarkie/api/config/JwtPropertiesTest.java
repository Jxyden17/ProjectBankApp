package nl.donniebankoebarkie.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void bindsJwtValuesFromSpringConfiguration() {
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", jwtProperties.secret());
        assertEquals(900L, jwtProperties.accessTokenExpirationSeconds());
        assertEquals(604800L, jwtProperties.refreshTokenExpirationSeconds());
    }
}

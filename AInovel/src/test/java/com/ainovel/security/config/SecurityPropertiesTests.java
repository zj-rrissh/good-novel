package com.ainovel.security.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTests {

    @Test
    void shouldRejectBlankJwtSecret() {
        assertThrows(IllegalStateException.class, () -> new SecurityProperties(
                List.of("/api/v1/auth/login"),
                Duration.ofHours(2),
                Duration.ofDays(7),
                "ainovel",
                "   ",
                true,
                true,
                20,
                60,
                10,
                60,
                120,
                60,
                40,
                300,
                20,
                300,
                200,
                300));
    }
}

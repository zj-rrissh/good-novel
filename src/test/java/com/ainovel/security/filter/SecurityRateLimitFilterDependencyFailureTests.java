package com.ainovel.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.rate.limiter.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "ainovel.security.rate-limit-enabled=true",
        "ainovel.security.risk-control-enabled=false"
})
class SecurityRateLimitFilterDependencyFailureTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.allow(anyString(), anyString(), any()))
                .thenThrow(new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis rate limit unavailable"));
    }

    @Test
    void shouldReturnDependencyUnavailableWhenRateLimiterFails() throws Exception {
        String payload = """
                {
                  "username": "missing_user",
                  "password": "wrong_password",
                  "deviceId": "device-rate-fail"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5001))
                .andExpect(jsonPath("$.message").value("dependency unavailable"));
    }
}

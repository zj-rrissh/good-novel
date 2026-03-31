package com.ainovel.security.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
        "ainovel.security.rate-limit-auth-threshold=100",
        "ainovel.security.rate-limit-auth-window-seconds=60",
        "ainovel.security.risk-control-enabled=true",
        "ainovel.security.risk-auth-threshold=1",
        "ainovel.security.risk-auth-window-seconds=300"
})
class SecurityRiskControlFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldBlockSecondAuthRequestWithRiskBlockedCode() throws Exception {
        String payload = """
                {
                  "username": "missing_user",
                  "password": "wrong_password",
                  "deviceId": "device-risk-1"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2102))
                .andExpect(jsonPath("$.message").value("risk blocked"));
    }
}

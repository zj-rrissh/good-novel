package com.ainovel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.service.AuthService;
import com.ainovel.user.vo.AccessTokenVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthSecurityRegressionTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from reading_progress");
        jdbcTemplate.update("delete from user_message");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");
    }

    @Test
    void shouldRejectPlaintextPasswordFallbackWhenHashIsNotBcrypt() {
        insertUserWithHash(1101L, "plain_user", "Password123");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("plain_user", "Password123", "device-a")));

        assertEquals(StandardErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void shouldRejectRefreshWhenRequestDeviceIdIsMissing() {
        AccessTokenVO token = authService.register(new RegisterRequest(
                "device_user",
                "Password123",
                "Device User",
                "https://example.com/avatar.png",
                "device-x"), "register-device");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refresh(new RefreshTokenRequest(token.refreshToken(), null)));

        assertEquals(StandardErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void shouldReturnInvalidClientHeaderWhenMissingXClient() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("invalid client header"));
    }

    private void insertUserWithHash(Long userId, String username, String passwordHash) {
        jdbcTemplate.update(
                """
                insert into user_account (id, username, password_hash, status, roles, login_version, created_at, updated_at)
                values (?, ?, ?, 'NORMAL', 'USER,AUTHOR', 1, current_timestamp, current_timestamp)
                """,
                userId,
                username,
                passwordHash);
        jdbcTemplate.update(
                """
                insert into user_profile (user_id, nickname, avatar_url, bio, level, verified_status, created_at, updated_at)
                values (?, ?, null, '', 1, 'UNVERIFIED', current_timestamp, current_timestamp)
                """,
                userId,
                username + "-nick");
    }
}

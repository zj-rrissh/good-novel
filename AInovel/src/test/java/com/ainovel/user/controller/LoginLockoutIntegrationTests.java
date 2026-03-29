package com.ainovel.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LoginLockoutIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Long USER_ID = 7901L;
    private static final String USERNAME = "lockout_user";
    private static final String CORRECT_PASSWORD = "Correct123";

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where username_attempt = ?", USERNAME);
        jdbcTemplate.update("delete from user_profile where user_id = ?", USER_ID);
        jdbcTemplate.update("delete from user_account where id = ?", USER_ID);
        jdbcTemplate.update("""
                insert into user_account (id, username, password_hash, status, roles, login_version,
                                         failed_login_count, locked_until)
                values (?, ?, ?, 'NORMAL', 'USER,AUTHOR', 1, 0, null)
                """, USER_ID, USERNAME, passwordEncoder.encode(CORRECT_PASSWORD));
        jdbcTemplate.update("""
                insert into user_profile (user_id, nickname, bio, level, verified_status)
                values (?, 'LockoutUser', '', 1, 'UNVERIFIED')
                """, USER_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where username_attempt = ?", USERNAME);
        jdbcTemplate.update("delete from user_profile where user_id = ?", USER_ID);
        jdbcTemplate.update("delete from user_account where id = ?", USER_ID);
    }

    private void attemptLogin(String password) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "username", USERNAME, "password", password));
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client", "pc-web")
                .content(body));
    }
    @Test
    void shouldSucceedLoginWithCorrectPassword() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "username", USERNAME, "password", CORRECT_PASSWORD));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void shouldIncrementFailedCountOnWrongPassword() throws Exception {
        attemptLogin("WrongPass1");
        Integer count = jdbcTemplate.queryForObject(
                "select failed_login_count from user_account where id = ?", Integer.class, USER_ID);
        assertEquals(1, count);
    }

    @Test
    void shouldLockAccountAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            attemptLogin("WrongPass");
        }
        Object lockedUntil = jdbcTemplate.queryForObject(
                "select locked_until from user_account where id = ?", Object.class, USER_ID);
        assertNotNull(lockedUntil);

        var body = objectMapper.writeValueAsString(Map.of(
                "username", USERNAME, "password", CORRECT_PASSWORD));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2004));
    }

    @Test
    void shouldRecordFailedLoginAttempt() throws Exception {
        attemptLogin("WrongPass");
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from user_login_record where username_attempt = ? and success = false",
                Long.class, USERNAME);
        assertEquals(1L, count);
    }

    @Test
    void shouldRecordSuccessfulLoginAttempt() throws Exception {
        attemptLogin(CORRECT_PASSWORD);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from user_login_record where username_attempt = ? and success = true",
                Long.class, USERNAME);
        assertEquals(1L, count);
    }

    @Test
    void shouldSetLockTriggeredOnFifthFailure() throws Exception {
        for (int i = 0; i < 5; i++) {
            attemptLogin("WrongPass");
        }
        Long lockTriggeredCount = jdbcTemplate.queryForObject(
                "select count(*) from user_login_record where username_attempt = ? and lock_triggered = true",
                Long.class, USERNAME);
        assertEquals(1L, lockTriggeredCount);
    }

    @Test
    void shouldResetFailedCountOnSuccessfulLogin() throws Exception {
        jdbcTemplate.update("update user_account set failed_login_count = 3 where id = ?", USER_ID);
        attemptLogin(CORRECT_PASSWORD);
        Integer count = jdbcTemplate.queryForObject(
                "select failed_login_count from user_account where id = ?", Integer.class, USER_ID);
        assertEquals(0, count);
    }
}

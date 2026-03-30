package com.ainovel.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.time.Instant;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class ChangePasswordControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Long USER_ID = 7701L;
    private static final String USERNAME = "pwtest_user";
    private static final String CURRENT_PASSWORD = "Password123";

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where username_attempt = ?", USERNAME);
        jdbcTemplate.update("delete from user_profile where user_id = ?", USER_ID);
        jdbcTemplate.update("delete from user_account where id = ?", USER_ID);
        jdbcTemplate.update("""
                insert into user_account (id, username, password_hash, status, roles, login_version)
                values (?, ?, ?, 'NORMAL', 'USER,AUTHOR', 1)
                """, USER_ID, USERNAME, passwordEncoder.encode(CURRENT_PASSWORD));
        jdbcTemplate.update("""
                insert into user_profile (user_id, nickname, bio, level, verified_status)
                values (?, 'PwTestUser', '', 1, 'UNVERIFIED')
                """, USER_ID);
        CurrentUserHolder.set(new CurrentUser(USER_ID, Set.of(RoleType.USER), null, true, "token-pw", 1L, Instant.now().plusSeconds(3600)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where username_attempt = ?", USERNAME);
        jdbcTemplate.update("delete from user_profile where user_id = ?", USER_ID);
        jdbcTemplate.update("delete from user_account where id = ?", USER_ID);
    }
    @Test
    void shouldChangePasswordSuccessfully() throws Exception {
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", CURRENT_PASSWORD,
                "newPassword", "NewPassword456"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String newHash = jdbcTemplate.queryForObject(
                "select password_hash from user_account where id = ?", String.class, USER_ID);
        assert passwordEncoder.matches("NewPassword456", newHash);
    }

    @Test
    void shouldRejectWrongCurrentPassword() throws Exception {
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", "WrongPassword",
                "newPassword", "NewPassword456"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void shouldRejectTooShortNewPassword() throws Exception {
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", CURRENT_PASSWORD,
                "newPassword", "short"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldRejectBlankCurrentPassword() throws Exception {
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", "",
                "newPassword", "NewPassword456"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldReturnUnauthenticatedWhenNoUser() throws Exception {
        CurrentUserHolder.clear();
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", CURRENT_PASSWORD,
                "newPassword", "NewPassword456"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void shouldIncrementLoginVersionAfterPasswordChange() throws Exception {
        Long versionBefore = jdbcTemplate.queryForObject(
                "select login_version from user_account where id = ?", Long.class, USER_ID);
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "currentPassword", CURRENT_PASSWORD,
                "newPassword", "NewPassword456"));
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client", "pc-web")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Long versionAfter = jdbcTemplate.queryForObject(
                "select login_version from user_account where id = ?", Long.class, USER_ID);
        assertEquals(versionBefore + 1, versionAfter);
    }
}


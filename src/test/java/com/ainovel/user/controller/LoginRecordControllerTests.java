package com.ainovel.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
class LoginRecordControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 8001L;
    private static final Long OTHER_USER_ID = 8002L;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where user_id in (?, ?)", USER_ID, OTHER_USER_ID);
        CurrentUserHolder.set(new CurrentUser(USER_ID, Set.of(RoleType.USER), null, true, "token-lr", 1L, Instant.now().plusSeconds(3600)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_login_record where user_id in (?, ?)", USER_ID, OTHER_USER_ID);
    }

    private void insertRecord(Long userId, boolean success, boolean lockTriggered) {
        jdbcTemplate.update("""
                insert into user_login_record (user_id, username_attempt, success, lock_triggered)
                values (?, 'testuser', ?, ?)
                """, userId, success, lockTriggered);
    }

    @Test
    void shouldReturnEmptyWhenNoRecords() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void shouldReturnUserLoginRecords() throws Exception {
        insertRecord(USER_ID, true, false);
        insertRecord(USER_ID, false, false);
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void shouldNotReturnOtherUsersRecords() throws Exception {
        insertRecord(OTHER_USER_ID, true, false);
        insertRecord(OTHER_USER_ID, false, false);
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldSupportPagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            insertRecord(USER_ID, i % 2 == 0, false);
        }
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    void shouldReturnUnauthenticatedWhenNoUser() throws Exception {
        CurrentUserHolder.clear();
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void shouldShowLockTriggeredField() throws Exception {
        insertRecord(USER_ID, false, true);
        mockMvc.perform(get("/api/v1/users/me/login-records").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].lockTriggered").value(true));
    }
}

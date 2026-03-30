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
class UnreadCountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 7801L;
    private static final Long OTHER_USER_ID = 7802L;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_message where to_user_id in (?, ?)", USER_ID, OTHER_USER_ID);
        CurrentUserHolder.set(new CurrentUser(USER_ID, Set.of(RoleType.USER), null, true, "token-unread", 1L, Instant.now().plusSeconds(3600)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from user_message where to_user_id in (?, ?)", USER_ID, OTHER_USER_ID);
    }

    private void insertMessage(Long toUserId, boolean read) {
        jdbcTemplate.update("""
                insert into user_message (to_user_id, type, title, content, read_at)
                values (?, 'SYSTEM', 'Test', 'content', ?)
                """, toUserId, read ? java.time.LocalDateTime.now() : null);
    }

    @Test
    void shouldReturnZeroWhenNoMessages() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/messages/unread-count").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void shouldCountOnlyUnreadMessages() throws Exception {
        insertMessage(USER_ID, false);
        insertMessage(USER_ID, false);
        insertMessage(USER_ID, true);
        mockMvc.perform(get("/api/v1/users/me/messages/unread-count").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    void shouldNotCountOtherUsersMessages() throws Exception {
        insertMessage(OTHER_USER_ID, false);
        insertMessage(OTHER_USER_ID, false);
        mockMvc.perform(get("/api/v1/users/me/messages/unread-count").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void shouldReturnCorrectCountAfterMarkRead() throws Exception {
        insertMessage(USER_ID, false);
        insertMessage(USER_ID, false);
        Long msgId = jdbcTemplate.queryForObject(
                "select id from user_message where to_user_id = ? and read_at is null fetch first 1 rows only",
                Long.class, USER_ID);
        jdbcTemplate.update("update user_message set read_at = current_timestamp(3) where id = ?", msgId);
        mockMvc.perform(get("/api/v1/users/me/messages/unread-count").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void shouldReturnUnauthenticatedWhenNoUser() throws Exception {
        CurrentUserHolder.clear();
        mockMvc.perform(get("/api/v1/users/me/messages/unread-count").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }
}

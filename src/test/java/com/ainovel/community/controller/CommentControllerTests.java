package com.ainovel.community.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CommentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from comment");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(2201L, "author_2201");
        insertUser(2202L, "reader_2202");
        insertNovel(8201L, 2201L);

        CurrentUserHolder.set(new CurrentUser(
                2202L,
                Set.of(RoleType.USER),
                "device-2202",
                true,
                "token-2202",
                1L,
                Instant.now().plusSeconds(600)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldCreateAndDeleteCommentViaController() throws Exception {
        String payload = """
                {
                  "targetType": "NOVEL",
                  "targetId": 8201,
                  "content": "hello comment",
                  "parentId": null,
                  "replyToUserId": null
                }
                """;

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "comment-create-1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("hello comment"));

        Long commentId = jdbcTemplate.queryForObject("select max(id) from comment", Long.class);
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String statusText = jdbcTemplate.queryForObject(
                "select status from comment where id = ?",
                String.class,
                commentId);
        assertEquals("DELETED", statusText);
    }

    @Test
    void shouldReturnInvalidRequestWhenRootCommentContainsReplyToUser() throws Exception {
        String payload = """
                {
                  "targetType": "NOVEL",
                  "targetId": 8201,
                  "content": "invalid root",
                  "parentId": null,
                  "replyToUserId": 2201
                }
                """;

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request"));
    }

    @Test
    void shouldReturnValidationFailedWhenCommentContentBlank() throws Exception {
        String payload = """
                {
                  "targetType": "NOVEL",
                  "targetId": 8201,
                  "content": "",
                  "parentId": null,
                  "replyToUserId": null
                }
                """;

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldReturnIdempotentConflictWhenIdempotencyKeyRepeated() throws Exception {
        String firstPayload = """
                {
                  "targetType": "NOVEL",
                  "targetId": 8201,
                  "content": "first comment",
                  "parentId": null,
                  "replyToUserId": null
                }
                """;

        String secondPayload = """
                {
                  "targetType": "NOVEL",
                  "targetId": 8201,
                  "content": "second comment",
                  "parentId": null,
                  "replyToUserId": null
                }
                """;

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "comment-idempotent-1")
                        .contentType("application/json")
                        .content(firstPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "comment-idempotent-1")
                        .contentType("application/json")
                        .content(secondPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.message").value("idempotent conflict"));
    }

    private void insertUser(Long userId, String username) {
        jdbcTemplate.update(
                """
                insert into user_account (id, username, password_hash, status, roles, login_version, created_at, updated_at)
                values (?, ?, ?, 'NORMAL', 'USER', 0, current_timestamp, current_timestamp)
                """,
                userId,
                username,
                "$2a$10$QJjYyx7y2du2Q1Wh0Lsp8usHfT3a6ICoJ56Q8M.8hM6otd2xTv2pu");
        jdbcTemplate.update(
                """
                insert into user_profile (user_id, nickname, avatar_url, bio, level, verified_status, created_at, updated_at)
                values (?, ?, null, null, 1, 'UNVERIFIED', current_timestamp, current_timestamp)
                """,
                userId,
                username + "-nick");
    }

    private void insertNovel(Long novelId, Long authorId) {
        jdbcTemplate.update(
                """
                insert into novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                                   latest_chapter_id, word_count, audit_task_id, created_at, updated_at)
                values (?, ?, ?, 'intro', null, null, null, 'ON_SHELF', null, 0, null, current_timestamp, current_timestamp)
                """,
                novelId,
                authorId,
                "novel-" + novelId);
    }
}

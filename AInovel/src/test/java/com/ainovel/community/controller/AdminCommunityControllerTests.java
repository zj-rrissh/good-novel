package com.ainovel.community.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
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
class AdminCommunityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from admin_operation_log");
        jdbcTemplate.update("delete from comment");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(9401L, "author_9401");
        insertUser(9402L, "reader_9402");
        insertUser(9403L, "reader_9403");
        insertNovel(8401L, 9401L);

        CurrentUserHolder.set(new CurrentUser(
                9400L,
                Set.of(RoleType.ADMIN),
                "admin-device",
                true,
                "admin-token",
                1L,
                Instant.now().plusSeconds(600)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldQueryCommunityCommentsWithStatusAndKeywordFilter() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertComment(8401L, 9402L, null, null, "命中关键字的评论", "VISIBLE", now.minusMinutes(2));
        insertComment(8401L, 9403L, null, null, "普通评论", "HIDDEN", now.minusMinutes(1));

        mockMvc.perform(get("/api/admin/v1/community/comments")
                        .header("X-Client", "admin")
                        .param("targetType", "NOVEL")
                        .param("targetId", "8401")
                        .param("status", "VISIBLE")
                        .param("keyword", "关键字")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].targetType").value("NOVEL"))
                .andExpect(jsonPath("$.data.records[0].targetId").value(8401))
                .andExpect(jsonPath("$.data.records[0].status").value("VISIBLE"))
                .andExpect(jsonPath("$.data.records[0].content").value("命中关键字的评论"));
    }

    @Test
    void shouldHideCommentFromCompatiblePathAndWriteOperationLog() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Long rootId = insertComment(8401L, 9402L, null, null, "root comment", "VISIBLE", now.minusMinutes(2));
        insertComment(8401L, 9403L, rootId, 9402L, "reply comment", "VISIBLE", now.minusMinutes(1));

        mockMvc.perform(post("/api/v1/admin/community/comments/{commentId}/hide", rootId)
                        .header("X-Client", "admin")
                        .header("X-Trace-Id", "trace-comment-hide-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/admin/community/comments")
                        .header("X-Client", "admin")
                        .param("targetType", "NOVEL")
                        .param("targetId", "8401")
                        .param("status", "HIDDEN")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        Integer operationLogCount = jdbcTemplate.queryForObject(
                """
                select count(1)
                from admin_operation_log
                where action = 'COMMENT_HIDE'
                  and biz_type = 'COMMENT'
                  and biz_id = ?
                  and operator_id = 9400
                  and trace_id = 'trace-comment-hide-1'
                """,
                Integer.class,
                rootId);
        assertEquals(1, operationLogCount);
    }

    private Long insertComment(Long targetId,
                               Long userId,
                               Long parentId,
                               Long replyToUserId,
                               String content,
                               String status,
                               LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                insert into comment (
                    target_type, target_id, user_id, parent_id, reply_to_user_id,
                    content, status, version, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "NOVEL",
                targetId,
                userId,
                parentId,
                replyToUserId,
                content,
                status,
                0L,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt));
        return jdbcTemplate.queryForObject("select max(id) from comment", Long.class);
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

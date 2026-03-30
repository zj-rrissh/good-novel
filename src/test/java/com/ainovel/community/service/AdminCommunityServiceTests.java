package com.ainovel.community.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.vo.CounterSummaryVO;
import com.ainovel.infrastructure.exception.BusinessException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdminCommunityServiceTests {

    @Autowired
    private AdminCommunityService adminCommunityService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from comment");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(9001L, "admin-comment-author");
        insertUser(9002L, "admin-comment-reply");
        insertNovel(9101L, 9001L);
    }

    @Test
    void shouldHideCommentTreeAndMakeItInvisibleForUserQueryAndCounter() {
        LocalDateTime now = LocalDateTime.now();
        Long rootId = insertComment(TargetType.NOVEL, 9101L, 9001L, null, null, "root", "VISIBLE", now.minusMinutes(2));
        Long replyId = insertComment(TargetType.NOVEL, 9101L, 9002L, rootId, 9001L, "reply", "VISIBLE", now.minusMinutes(1));

        adminCommunityService.hideComment(rootId);

        Long rootStatusCount = jdbcTemplate.queryForObject(
                "select count(1) from comment where id = ? and status = 'HIDDEN'",
                Long.class,
                rootId);
        Long replyStatusCount = jdbcTemplate.queryForObject(
                "select count(1) from comment where id = ? and status = 'HIDDEN'",
                Long.class,
                replyId);
        assertEquals(1L, rootStatusCount == null ? 0L : rootStatusCount);
        assertEquals(1L, replyStatusCount == null ? 0L : replyStatusCount);

        assertEquals(0L, commentService.queryComments(TargetType.NOVEL, 9101L, 1, 20, "new").total());

        CounterSummaryVO counters = counterService.getCounters(TargetType.NOVEL, 9101L);
        assertEquals(0L, counters.commentCount());
    }

    @Test
    void shouldBeIdempotentForHiddenAndDeletedComments() {
        LocalDateTime now = LocalDateTime.now();
        Long commentId = insertComment(TargetType.NOVEL, 9101L, 9001L, null, null, "visible", "VISIBLE", now.minusMinutes(1));

        assertDoesNotThrow(() -> adminCommunityService.hideComment(commentId));
        assertDoesNotThrow(() -> adminCommunityService.hideComment(commentId));

        jdbcTemplate.update("update comment set status = 'DELETED' where id = ?", commentId);
        assertDoesNotThrow(() -> adminCommunityService.hideComment(commentId));
    }

    @Test
    void shouldRejectHideWhenCommentNotExists() {
        BusinessException exception = assertThrows(BusinessException.class, () -> adminCommunityService.hideComment(999999L));
        assertEquals(StandardErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private Long insertComment(TargetType targetType,
                               Long targetId,
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
                targetType.name(),
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

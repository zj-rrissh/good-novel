package com.ainovel.community.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.ToggleReactionRequest;
import com.ainovel.community.vo.CounterSummaryVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ReactionAndCounterServiceTests {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from reaction");
        jdbcTemplate.update("delete from user_follow");
        jdbcTemplate.update("delete from comment");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(101L, "author_user");
        insertUser(102L, "reader_user");
        insertUser(103L, "peer_user");

        insertNovel(501L, 101L, "on-shelf-novel", "ON_SHELF");
        insertChapter(701L, 501L, 1, "published-chapter", "PUBLISHED");

        LocalDateTime now = LocalDateTime.now();
        insertComment(TargetType.NOVEL, 501L, 101L, "visible-a", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 501L, 102L, "visible-b", "VISIBLE", now.minusMinutes(2));
        insertComment(TargetType.NOVEL, 501L, 103L, "deleted-c", "DELETED", now.minusMinutes(1));

        loginAs(102L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldPersistLikeFavoriteFollowWithIdempotentToggle() {
        ToggleReactionRequest novelTarget = new ToggleReactionRequest(TargetType.NOVEL, 501L);

        reactionService.like(novelTarget);
        reactionService.like(novelTarget);
        assertEquals(1L, countRows("reaction", "reaction_type = 'LIKE' and target_type = 'NOVEL' and target_id = 501 and user_id = 102"));
        assertEquals("ACTIVE", queryString("select status from reaction where reaction_type = 'LIKE' and target_type = 'NOVEL' and target_id = 501 and user_id = 102"));

        reactionService.unlike(novelTarget);
        reactionService.unlike(novelTarget);
        assertEquals("CANCELED", queryString("select status from reaction where reaction_type = 'LIKE' and target_type = 'NOVEL' and target_id = 501 and user_id = 102"));

        ToggleReactionRequest chapterTarget = new ToggleReactionRequest(TargetType.CHAPTER, 701L);
        reactionService.favorite(chapterTarget);
        reactionService.favorite(chapterTarget);
        assertEquals(1L, countRows("reaction", "reaction_type = 'FAVORITE' and target_type = 'CHAPTER' and target_id = 701 and user_id = 102"));
        assertEquals("ACTIVE", queryString("select status from reaction where reaction_type = 'FAVORITE' and target_type = 'CHAPTER' and target_id = 701 and user_id = 102"));

        reactionService.unfavorite(chapterTarget);
        reactionService.unfavorite(chapterTarget);
        assertEquals("CANCELED", queryString("select status from reaction where reaction_type = 'FAVORITE' and target_type = 'CHAPTER' and target_id = 701 and user_id = 102"));

        reactionService.follow(101L);
        reactionService.follow(101L);
        assertEquals(1L, countRows("user_follow", "user_id = 102 and target_user_id = 101"));
        assertEquals("ACTIVE", queryString("select status from user_follow where user_id = 102 and target_user_id = 101"));

        reactionService.unfollow(101L);
        reactionService.unfollow(101L);
        assertEquals("CANCELED", queryString("select status from user_follow where user_id = 102 and target_user_id = 101"));
    }

    @Test
    void shouldReturnCounterSummaryAndEchoCurrentUserState() {
        loginAs(102L);
        reactionService.like(new ToggleReactionRequest(TargetType.NOVEL, 501L));
        reactionService.favorite(new ToggleReactionRequest(TargetType.NOVEL, 501L));
        reactionService.follow(101L);

        loginAs(103L);
        reactionService.like(new ToggleReactionRequest(TargetType.NOVEL, 501L));
        reactionService.follow(101L);

        loginAs(102L);
        CounterSummaryVO summary = counterService.getCounters(TargetType.NOVEL, 501L);

        assertEquals(TargetType.NOVEL.name(), summary.targetType());
        assertEquals(501L, summary.targetId());
        assertEquals(2L, summary.commentCount());
        assertEquals(2L, summary.likeCount());
        assertEquals(1L, summary.favoriteCount());
        assertEquals(2L, summary.followerCount());
        assertTrue(readBoolean(summary, "liked"));
        assertTrue(readBoolean(summary, "favorited"));
        assertTrue(readBoolean(summary, "followed"));

        CurrentUserHolder.clear();
        CounterSummaryVO anonymousSummary = counterService.getCounters(TargetType.NOVEL, 501L);
        assertFalse(readBoolean(anonymousSummary, "liked"));
        assertFalse(readBoolean(anonymousSummary, "favorited"));
        assertFalse(readBoolean(anonymousSummary, "followed"));
    }

    @Test
    void shouldRejectFollowSelf() {
        loginAs(102L);
        BusinessException exception = assertThrows(BusinessException.class, () -> reactionService.follow(102L));
        assertEquals(StandardErrorCode.BUSINESS_STATE_INVALID, exception.getErrorCode());
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

    private void insertNovel(Long novelId, Long authorId, String title, String status) {
        jdbcTemplate.update(
                """
                        insert into novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                                           latest_chapter_id, word_count, audit_task_id, created_at, updated_at)
                        values (?, ?, ?, 'intro', null, null, null, ?, null, 0, null, current_timestamp, current_timestamp)
                        """,
                novelId,
                authorId,
                title,
                status);
    }

    private void insertChapter(Long chapterId, Long novelId, int chapterNo, String title, String status) {
        jdbcTemplate.update(
                """
                        insert into novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id,
                                                   published_at, created_at, updated_at)
                        values (?, ?, ?, ?, 'content', ?, null, current_timestamp, current_timestamp, current_timestamp)
                        """,
                chapterId,
                novelId,
                chapterNo,
                title,
                status);
    }

    private void insertComment(TargetType targetType,
                               Long targetId,
                               Long userId,
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
                null,
                null,
                content,
                status,
                0L,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt));
    }

    private long countRows(String table, String whereClause) {
        Long count = jdbcTemplate.queryForObject("select count(1) from " + table + " where " + whereClause, Long.class);
        return count == null ? 0L : count;
    }

    private String queryString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private boolean readBoolean(CounterSummaryVO summary, String accessor) {
        try {
            Object value = summary.getClass().getMethod(accessor).invoke(summary);
            return value instanceof Boolean bool && bool;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            fail("CounterSummaryVO missing accessor: " + accessor + "(), cause=" + ex.getClass().getSimpleName());
            return false;
        }
    }

    private void loginAs(Long userId) {
        CurrentUserHolder.set(new CurrentUser(
                userId,
                Set.of(RoleType.USER),
                "test-device",
                true,
                "token-" + userId,
                1L,
                Instant.now().plusSeconds(600)));
    }
}

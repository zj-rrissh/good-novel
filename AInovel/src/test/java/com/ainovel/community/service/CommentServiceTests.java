package com.ainovel.community.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.vo.CommentVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
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
class CommentServiceTests {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from comment");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");
        CurrentUserHolder.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldReadVisibleCommentsAndTotalFromDatabase() {
        insertUser(11L, "comment-author-11");
        insertUser(12L, "comment-author-12");
        insertUser(13L, "comment-author-13");
        insertUser(14L, "comment-author-14");
        insertUser(15L, "comment-author-15");
        insertNovel(1001L, 11L);

        LocalDateTime now = LocalDateTime.now();
        insertComment(TargetType.NOVEL, 1001L, 11L, null, null, "first", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 1001L, 12L, null, null, "second", "VISIBLE", now.minusMinutes(2));
        insertComment(TargetType.NOVEL, 1001L, 13L, null, null, "third", "VISIBLE", now.minusMinutes(1));
        insertComment(TargetType.NOVEL, 1001L, 14L, null, null, "hidden", "DELETED", now);
        insertComment(TargetType.CHAPTER, 1001L, 15L, null, null, "other-target", "VISIBLE", now);

        PageResponse<CommentVO> page = commentService.queryComments(TargetType.NOVEL, 1001L, 1, 2, "new");

        assertEquals(3L, page.total());
        assertEquals(2, page.records().size());
        assertEquals(List.of("third", "second"), page.records().stream().map(CommentVO::content).toList());
    }

    @Test
    void shouldPageRootCommentsAndAttachReplies() {
        insertUser(21L, "root-author-21");
        insertUser(22L, "root-author-22");
        insertUser(23L, "reply-author-23");
        insertUser(24L, "reply-author-24");
        insertNovel(2001L, 21L);

        LocalDateTime now = LocalDateTime.now();
        Long rootOldId = insertComment(TargetType.NOVEL, 2001L, 21L, null, null, "root-old", "VISIBLE", now.minusMinutes(4));
        insertComment(TargetType.NOVEL, 2001L, 23L, rootOldId, 21L, "reply-old-1", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 2001L, 24L, rootOldId, 21L, "reply-old-2", "VISIBLE", now.minusMinutes(2));

        insertComment(TargetType.NOVEL, 2001L, 22L, null, null, "root-new", "VISIBLE", now.minusMinutes(1));

        PageResponse<CommentVO> page = commentService.queryComments(TargetType.NOVEL, 2001L, 1, 1, "new");

        assertEquals(2L, page.total());
        assertEquals(1, page.records().size());
        assertEquals("root-new", page.records().get(0).content());
        assertEquals(0, page.records().get(0).replies().size());

        PageResponse<CommentVO> oldPage = commentService.queryComments(TargetType.NOVEL, 2001L, 1, 2, "old");
        assertEquals(List.of("root-old", "root-new"), oldPage.records().stream().map(CommentVO::content).toList());
        assertEquals(List.of("reply-old-1", "reply-old-2"),
                oldPage.records().get(0).replies().stream().map(CommentVO::content).toList());
    }

    @Test
    void shouldRespectSortOldAndFallbackToNewWhenSortUnsupported() {
        insertUser(31L, "sort-author-31");
        insertUser(32L, "sort-author-32");
        insertUser(33L, "sort-author-33");
        insertNovel(1002L, 31L);

        LocalDateTime now = LocalDateTime.now();
        insertComment(TargetType.NOVEL, 1002L, 31L, null, null, "first", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 1002L, 32L, null, null, "second", "VISIBLE", now.minusMinutes(2));
        insertComment(TargetType.NOVEL, 1002L, 33L, null, null, "third", "VISIBLE", now.minusMinutes(1));

        PageResponse<CommentVO> oldSorted = commentService.queryComments(TargetType.NOVEL, 1002L, 1, 3, "old");
        assertEquals(List.of("first", "second", "third"), oldSorted.records().stream().map(CommentVO::content).toList());

        PageResponse<CommentVO> fallbackSorted = commentService.queryComments(TargetType.NOVEL, 1002L, 1, 3, "unsupported");
        assertEquals(List.of("third", "second", "first"),
                fallbackSorted.records().stream().map(CommentVO::content).toList());
    }

    @Test
    void shouldRejectQueryWhenTargetDoesNotExist() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.queryComments(TargetType.NOVEL, 4040L, 1, 20, "new"));
        assertEquals(StandardErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void shouldReflectDatabaseDeleteStateAcrossInstances() {
        insertUser(41L, "delete-author-41");
        insertNovel(1003L, 41L);
        loginAs(41L);

        CommentVO created = commentService.createComment(
                new CreateCommentRequest(TargetType.NOVEL, 1003L, "content", null, null),
                "comment-create-1");

        PageResponse<CommentVO> beforeDelete = commentService.queryComments(TargetType.NOVEL, 1003L, 1, 10, "new");
        assertEquals(1L, beforeDelete.total());

        jdbcTemplate.update(
                "update comment set status = 'DELETED', version = version + 1 where id = ?",
                created.commentId());

        PageResponse<CommentVO> afterDelete = commentService.queryComments(TargetType.NOVEL, 1003L, 1, 10, "new");
        assertEquals(0L, afterDelete.total());
    }

    @Test
    void shouldRejectDuplicateSubmissionWithinWindow() {
        insertUser(51L, "dup-author-51");
        insertNovel(1004L, 51L);
        loginAs(51L);

        commentService.createComment(
                new CreateCommentRequest(TargetType.NOVEL, 1004L, "same content", null, null),
                "comment-create-2");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.createComment(
                        new CreateCommentRequest(TargetType.NOVEL, 1004L, " same   content ", null, null),
                        "comment-create-3"));
        assertEquals(StandardErrorCode.IDEMPOTENT_CONFLICT, exception.getErrorCode());
    }

    @Test
    void shouldRejectRootCommentWithReplyToUserId() {
        insertUser(61L, "root-reply-61");
        insertUser(62L, "root-reply-62");
        insertNovel(1005L, 61L);
        loginAs(61L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.createComment(
                        new CreateCommentRequest(TargetType.NOVEL, 1005L, "root invalid", null, 62L),
                        "comment-create-4"));
        assertEquals(StandardErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void shouldRejectReplyWhenParentTargetMismatch() {
        insertUser(71L, "parent-mismatch-71");
        insertUser(72L, "parent-mismatch-72");
        insertNovel(1006L, 71L);
        insertNovel(1007L, 72L);
        Long parentId = insertComment(
                TargetType.NOVEL,
                1006L,
                71L,
                null,
                null,
                "parent-1006",
                "VISIBLE",
                LocalDateTime.now().minusMinutes(1));

        loginAs(72L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.createComment(
                        new CreateCommentRequest(TargetType.NOVEL, 1007L, "reply invalid", parentId, 71L),
                        "comment-create-5"));
        assertEquals(StandardErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void shouldRejectReplyWhenReplyToUserDoesNotMatchParentAuthor() {
        insertUser(81L, "parent-author-81");
        insertUser(82L, "reply-author-82");
        insertUser(83L, "wrong-reply-target-83");
        insertNovel(1008L, 81L);
        Long parentId = insertComment(
                TargetType.NOVEL,
                1008L,
                81L,
                null,
                null,
                "parent",
                "VISIBLE",
                LocalDateTime.now().minusMinutes(1));

        loginAs(82L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.createComment(
                        new CreateCommentRequest(TargetType.NOVEL, 1008L, "reply invalid", parentId, 83L),
                        "comment-create-6"));
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

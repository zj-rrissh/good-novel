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
        CurrentUserHolder.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldReadVisibleCommentsAndTotalFromDatabase() {
        LocalDateTime now = LocalDateTime.now();
        insertComment(TargetType.NOVEL, 1001L, 11L, "first", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 1001L, 12L, "second", "VISIBLE", now.minusMinutes(2));
        insertComment(TargetType.NOVEL, 1001L, 13L, "third", "VISIBLE", now.minusMinutes(1));
        insertComment(TargetType.NOVEL, 1001L, 14L, "hidden", "DELETED", now);
        insertComment(TargetType.CHAPTER, 1001L, 15L, "other-target", "VISIBLE", now);

        PageResponse<CommentVO> page = commentService.queryComments(TargetType.NOVEL, 1001L, 1, 2, "new");

        assertEquals(3L, page.total());
        assertEquals(2, page.records().size());
        assertEquals(List.of("third", "second"), page.records().stream().map(CommentVO::content).toList());
    }

    @Test
    void shouldRespectSortOldAndFallbackToNewWhenSortUnsupported() {
        LocalDateTime now = LocalDateTime.now();
        insertComment(TargetType.NOVEL, 1002L, 11L, "first", "VISIBLE", now.minusMinutes(3));
        insertComment(TargetType.NOVEL, 1002L, 12L, "second", "VISIBLE", now.minusMinutes(2));
        insertComment(TargetType.NOVEL, 1002L, 13L, "third", "VISIBLE", now.minusMinutes(1));

        PageResponse<CommentVO> oldSorted = commentService.queryComments(TargetType.NOVEL, 1002L, 1, 3, "old");
        assertEquals(List.of("first", "second", "third"), oldSorted.records().stream().map(CommentVO::content).toList());

        PageResponse<CommentVO> fallbackSorted = commentService.queryComments(TargetType.NOVEL, 1002L, 1, 3, "unsupported");
        assertEquals(List.of("third", "second", "first"),
                fallbackSorted.records().stream().map(CommentVO::content).toList());
    }

    @Test
    void shouldReflectDatabaseDeleteStateAcrossInstances() {
        loginAs(21L);
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
        loginAs(31L);
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

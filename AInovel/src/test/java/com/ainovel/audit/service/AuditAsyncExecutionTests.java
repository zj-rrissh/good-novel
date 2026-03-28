package com.ainovel.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ainovel.audit.domain.ReviewDecision;
import com.ainovel.audit.domain.RiskLevel;
import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.SubmitNovelAuditRequest;
import com.ainovel.novel.service.ChapterManagementService;
import com.ainovel.novel.service.NovelAuditService;
import com.ainovel.novel.service.NovelDraftService;
import com.ainovel.novel.vo.NovelChapterVO;
import com.ainovel.novel.vo.NovelDetailVO;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.token.AccessTokenClaims;
import com.ainovel.security.auth.token.JwtTokenProvider;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.service.AuthService;
import com.ainovel.user.vo.AccessTokenVO;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditAsyncExecutionTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private NovelDraftService novelDraftService;

    @Autowired
    private ChapterManagementService chapterManagementService;

    @Autowired
    private NovelAuditService novelAuditService;

    @Autowired
    private AuditTaskMapper auditTaskMapper;

    @Autowired
    private ManualReviewService manualReviewService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldDispatchSubmittedNovelAuditTaskAsynchronously() throws InterruptedException {
        String suffix = uniqueSuffix();
        loginAsAuthor(suffix);
        SubmissionScenario scenario = createSubmissionScenario(suffix);

        long startedAt = System.nanoTime();
        String taskId = novelAuditService.submitAudit(
                scenario.novel().novelId(),
                new SubmitNovelAuditRequest(Set.of(scenario.chapter().chapterId()), "async submit"),
                "audit-" + suffix);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertNotNull(taskId);
        assertTrue(elapsedMs < 1_000, "submitAudit should return before async execution finishes");

        AuditTaskEntity task = waitForManualReview(Long.valueOf(taskId));

        assertEquals(AuditStatus.MANUAL_REVIEW, task.getAuditStatus());
        assertTrue(
                Set.of("phase1_async_manual_review", "snapshot_empty").contains(task.getReasonCode()),
                "expected async executor reason code");
    }

    @Test
    void shouldNotOverwriteReviewedTaskWhenAsyncWritebackRunsLate() {
        String suffix = uniqueSuffix();
        loginAsAuthor(suffix);
        SubmissionScenario scenario = createSubmissionScenario(suffix);

        String taskId = novelAuditService.submitAudit(
                scenario.novel().novelId(),
                new SubmitNovelAuditRequest(Set.of(scenario.chapter().chapterId()), "async submit"),
                "audit-" + suffix);

        manualReviewService.review(
                Long.valueOf(taskId),
                new ReviewAuditTaskRequest(ReviewDecision.REJECT, "manual_review", "manual review wins"));

        int updatedRows = auditTaskMapper.updateExecutionResult(
                Long.valueOf(taskId),
                AuditStatus.MANUAL_REVIEW,
                RiskLevel.MEDIUM,
                "phase1_async_manual_review",
                "late async write");
        AuditTaskEntity task = auditTaskMapper.findById(Long.valueOf(taskId));

        assertEquals(0, updatedRows);
        assertEquals(AuditStatus.REJECT, task.getAuditStatus());
    }

    private AuditTaskEntity waitForManualReview(Long taskId) throws InterruptedException {
        AuditTaskEntity latest = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            latest = auditTaskMapper.findById(taskId);
            if (latest != null && latest.getAuditStatus() == AuditStatus.MANUAL_REVIEW) {
                return latest;
            }
            Thread.sleep(50);
        }
        return latest;
    }

    private SubmissionScenario createSubmissionScenario(String suffix) {
        NovelDetailVO novel = novelDraftService.createNovel(
                new CreateNovelRequest(
                        "Async Novel " + suffix,
                        "intro " + suffix,
                        "https://example.com/async-cover-" + suffix + ".png",
                        1L,
                        Set.of(1L, 2L)),
                "novel-" + suffix);
        NovelChapterVO chapter = chapterManagementService.createChapter(
                novel.novelId(),
                new CreateChapterRequest(1, "chapter " + suffix, "story content " + suffix),
                "chapter-" + suffix);
        return new SubmissionScenario(novel, chapter);
    }

    private void loginAsAuthor(String suffix) {
        AccessTokenVO token = authService.register(
                new RegisterRequest(
                        "asyncauthor" + suffix,
                        "Password123",
                        "Async Author " + suffix,
                        "https://example.com/" + suffix + ".png",
                        "device-" + suffix),
                "register-" + suffix);
        CurrentUserHolder.set(toCurrentUser(token.accessToken()));
    }

    private CurrentUser toCurrentUser(String accessToken) {
        AccessTokenClaims claims = jwtTokenProvider.parse(accessToken).orElseThrow();
        return new CurrentUser(
                Long.valueOf(claims.subject()),
                claims.roles(),
                "device",
                true,
                claims.jti(),
                claims.tokenVersion(),
                claims.expiresAt());
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record SubmissionScenario(NovelDetailVO novel, NovelChapterVO chapter) {
    }
}

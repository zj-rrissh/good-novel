package com.ainovel.novel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.audit.domain.ReviewDecision;
import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.service.ManualReviewService;
import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.SubmitNovelAuditRequest;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NovelAuditServiceTests {

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
    private ManualReviewService manualReviewService;

    @Autowired
    private NovelMapper novelMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private AuditTaskMapper auditTaskMapper;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldCreatePendingAuditTaskForNovelIntro() {
        String suffix = uniqueSuffix();
        loginAsAuthor(suffix);
        SubmissionScenario scenario = createSubmissionScenario(suffix);

        String auditTaskId = submitAudit(scenario, "first submit", "audit-" + suffix);

        NovelEntity persistedNovel = novelMapper.findById(scenario.novel().novelId());
        ChapterEntity persistedChapter = chapterMapper.findById(scenario.chapter().chapterId());
        AuditTaskEntity task = auditTaskMapper.findById(Long.valueOf(auditTaskId));

        assertNotNull(task);
        assertEquals(AuditStatus.PENDING, task.getAuditStatus());
        assertEquals(NovelStatus.PENDING_AUDIT, persistedNovel.getStatus());
        assertEquals(auditTaskId, persistedNovel.getAuditTaskId());
        assertEquals(ChapterStatus.PENDING_AUDIT, persistedChapter.getStatus());
        assertEquals(auditTaskId, persistedChapter.getAuditTaskId());
    }

    @Test
    void shouldReuseUnfinishedNovelAuditTask() {
        String suffix = uniqueSuffix();
        loginAsAuthor(suffix);
        SubmissionScenario scenario = createSubmissionScenario(suffix);

        String firstTaskId = submitAudit(scenario, "same payload", "audit-" + suffix);
        String reusedTaskId = submitAudit(scenario, "same payload", "audit-retry-" + suffix);

        assertEquals(firstTaskId, reusedTaskId);
    }

    @Test
    void shouldReturnExistingTerminalTaskForSameHash() {
        String suffix = uniqueSuffix();
        loginAsAuthor(suffix);
        SubmissionScenario scenario = createSubmissionScenario(suffix);

        String firstTaskId = submitAudit(scenario, "same payload", "audit-" + suffix);
        assertNotNull(firstTaskId);

        manualReviewService.review(
                Long.valueOf(firstTaskId),
                new ReviewAuditTaskRequest(ReviewDecision.REJECT, "needs_changes", "retry same content"));

        String reusedTaskId = submitAudit(scenario, "same payload", "audit-retry-" + suffix);

        assertEquals(firstTaskId, reusedTaskId);
    }

    private SubmissionScenario createSubmissionScenario(String suffix) {
        NovelDetailVO novel = novelDraftService.createNovel(
                new CreateNovelRequest(
                        "Novel " + suffix,
                        "intro " + suffix,
                        "https://example.com/cover-" + suffix + ".png",
                        1L,
                        Set.of(1L, 2L)),
                "novel-" + suffix);
        NovelChapterVO chapter = chapterManagementService.createChapter(
                novel.novelId(),
                new CreateChapterRequest(1, "chapter " + suffix, "story content " + suffix),
                "chapter-" + suffix);
        return new SubmissionScenario(novel, chapter);
    }

    private String submitAudit(SubmissionScenario scenario, String reason, String idempotencyKey) {
        return novelAuditService.submitAudit(
                scenario.novel().novelId(),
                new SubmitNovelAuditRequest(Set.of(scenario.chapter().chapterId()), reason),
                idempotencyKey);
    }

    private void loginAsAuthor(String suffix) {
        AccessTokenVO token = authService.register(
                new RegisterRequest(
                        "author" + suffix,
                        "Password123",
                        "Author " + suffix,
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

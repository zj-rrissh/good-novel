package com.ainovel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ainovel.audit.domain.ReviewDecision;
import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.service.ManualReviewService;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.SubmitNovelAuditRequest;
import com.ainovel.novel.service.ChapterManagementService;
import com.ainovel.novel.service.NovelAuditService;
import com.ainovel.novel.service.NovelDraftService;
import com.ainovel.novel.service.NovelShelfService;
import com.ainovel.novel.vo.NovelChapterVO;
import com.ainovel.novel.vo.NovelDetailVO;
import com.ainovel.reading.dto.UpdateReadingProgressRequest;
import com.ainovel.reading.service.ChapterReadService;
import com.ainovel.reading.service.NovelReadService;
import com.ainovel.reading.service.ProgressService;
import com.ainovel.reading.vo.ChapterContentVO;
import com.ainovel.reading.vo.ReadingProgressVO;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.token.AccessTokenClaims;
import com.ainovel.security.auth.token.JwtTokenProvider;
import com.ainovel.user.domain.MessageType;
import com.ainovel.user.dto.DeliverMessageRequest;
import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.MarkMessagesReadRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.dto.UpdateUserProfileRequest;
import com.ainovel.user.service.AuthService;
import com.ainovel.user.service.UserMessageService;
import com.ainovel.user.service.UserProfileService;
import com.ainovel.user.vo.AccessTokenVO;
import com.ainovel.user.vo.UserMeVO;
import com.ainovel.user.vo.UserProfileVO;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoreBusinessFlowTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMessageService userMessageService;

    @Autowired
    private NovelDraftService novelDraftService;

    @Autowired
    private ChapterManagementService chapterManagementService;

    @Autowired
    private NovelAuditService novelAuditService;

    @Autowired
    private ManualReviewService manualReviewService;

    @Autowired
    private NovelShelfService novelShelfService;

    @Autowired
    private NovelReadService novelReadService;

    @Autowired
    private ChapterReadService chapterReadService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldCompleteCoreBusinessFlow() {
        AccessTokenVO registerToken = authService.register(new RegisterRequest(
                "author001",
                "Password123",
                "Author One",
                "https://example.com/a1.png",
                "device1"), "register-1");
        assertNotNull(registerToken.accessToken());
        assertNotNull(registerToken.refreshToken());

        AccessTokenVO refreshedToken = authService.refresh(new RefreshTokenRequest(registerToken.refreshToken(), null));
        assertNotNull(refreshedToken.accessToken());

        CurrentUserHolder.set(toCurrentUser(refreshedToken.accessToken()));
        authService.logout();
        BusinessException refreshAfterLogout = assertThrows(
                BusinessException.class,
                () -> authService.refresh(new RefreshTokenRequest(refreshedToken.refreshToken(), null)));
        assertEquals(StandardErrorCode.UNAUTHENTICATED, refreshAfterLogout.getErrorCode());

        AccessTokenVO loginToken = authService.login(new LoginRequest("author001", "Password123", "device1"));
        CurrentUserHolder.set(toCurrentUser(loginToken.accessToken()));

        UserMeVO me = userProfileService.currentUser();
        assertEquals("author001", me.username());

        UserProfileVO updatedProfile = userProfileService.updateProfile(
                new UpdateUserProfileRequest("Author Prime", "https://example.com/a1-new.png", "new bio"));
        assertEquals("Author Prime", updatedProfile.nickname());

        userMessageService.deliver("msg-1", new DeliverMessageRequest(
                me.userId(),
                MessageType.SYSTEM,
                "welcome",
                "hello",
                "SYSTEM",
                1L,
                "test",
                "trace-1"));
        var unreadMessages = userMessageService.queryMessages(me.userId(), 1, 10, null, false);
        assertEquals(1, unreadMessages.total());
        userMessageService.markRead(me.userId(), new MarkMessagesReadRequest(Set.of(unreadMessages.records().get(0).messageId())));
        assertEquals(1, userMessageService.queryMessages(me.userId(), 1, 10, null, true).total());

        NovelDetailVO novel = novelDraftService.createNovel(
                new CreateNovelRequest("Novel One", "intro", "https://example.com/cover.png", 1L, Set.of(1L, 2L)),
                "novel-1");
        NovelChapterVO chapter = chapterManagementService.createChapter(
                novel.novelId(),
                new CreateChapterRequest(1, "chapter one", "story content"),
                "chapter-1");
        String auditTaskId = novelAuditService.submitAudit(
                novel.novelId(),
                new SubmitNovelAuditRequest(Set.of(chapter.chapterId()), "first submit"),
                "audit-1");
        manualReviewService.review(Long.valueOf(auditTaskId), new ReviewAuditTaskRequest(ReviewDecision.PASS, null, null));
        novelShelfService.onShelf(novel.novelId(), "publish");

        var readDetail = novelReadService.getNovelDetail(novel.novelId());
        assertEquals("Novel One", readDetail.title());
        assertEquals(1, novelReadService.getChapterPage(novel.novelId(), 1, 10).records().size());

        ChapterContentVO content = chapterReadService.getChapterContent(chapter.chapterId());
        assertEquals("story content", content.content());

        ReadingProgressVO saved = progressService.saveProgress(
                me.userId(),
                new UpdateReadingProgressRequest(novel.novelId(), chapter.chapterId(), 60, System.currentTimeMillis(), null, 128L),
                "progress-1");
        assertEquals(60, saved.progressPercent());
        assertEquals(chapter.chapterId(), progressService.getProgress(me.userId(), novel.novelId()).chapterId());

        novelShelfService.offShelf(novel.novelId(), "maintenance");
        BusinessException hiddenError = assertThrows(
                BusinessException.class,
                () -> novelReadService.getNovelDetail(novel.novelId()));
        assertEquals(StandardErrorCode.CONTENT_NOT_VISIBLE, hiddenError.getErrorCode());
    }

    private CurrentUser toCurrentUser(String accessToken) {
        AccessTokenClaims claims = jwtTokenProvider.parse(accessToken).orElseThrow();
        return new CurrentUser(
                Long.valueOf(claims.subject()),
                claims.roles(),
                "unknown",
                true,
                claims.jti(),
                claims.tokenVersion(),
                claims.expiresAt());
    }
}

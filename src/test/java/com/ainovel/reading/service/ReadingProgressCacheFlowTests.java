package com.ainovel.reading.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.reading.dto.UpdateReadingProgressRequest;
import com.ainovel.reading.vo.ReadingProgressVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ReadingProgressCacheFlowTests {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private CacheKeyFactory cacheKeyFactory;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from reading_progress");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(3201L, "cache_reader_3201");
        insertNovel(9201L, 3201L);
        insertChapter(9301L, 9201L);
    }

    @Test
    void shouldPopulateAndInvalidateReadingProgressCacheAcrossReadWrite() throws Exception {
        Long userId = 3201L;
        Long novelId = 9201L;
        Long chapterId = 9301L;
        String cacheKey = cacheKeyFactory.readingProgress(userId, novelId);

        progressService.saveProgress(
                userId,
                new UpdateReadingProgressRequest(novelId, chapterId, 45, System.currentTimeMillis(), "device-cache", 120L),
                "cache-progress-1");
        assertNull(redisTemplate.opsForValue().get(cacheKey));

        ReadingProgressVO firstRead = progressService.getProgress(userId, novelId);
        assertEquals(45, firstRead.progressPercent());
        String firstCachePayload = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(firstCachePayload);
        JsonNode firstCacheJson = objectMapper.readTree(firstCachePayload);
        assertEquals(45, firstCacheJson.get("progressPercent").asInt());

        progressService.saveProgress(
                userId,
                new UpdateReadingProgressRequest(novelId, chapterId, 80, System.currentTimeMillis(), "device-cache", 256L),
                "cache-progress-2");
        assertNull(redisTemplate.opsForValue().get(cacheKey));

        ReadingProgressVO secondRead = progressService.getProgress(userId, novelId);
        assertEquals(80, secondRead.progressPercent());
        String secondCachePayload = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(secondCachePayload);
        JsonNode secondCacheJson = objectMapper.readTree(secondCachePayload);
        assertEquals(80, secondCacheJson.get("progressPercent").asInt());
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

    private void insertChapter(Long chapterId, Long novelId) {
        jdbcTemplate.update(
                """
                insert into novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id,
                                           published_at, created_at, updated_at)
                values (?, ?, 1, ?, 'content', 'PUBLISHED', null, current_timestamp, current_timestamp, current_timestamp)
                """,
                chapterId,
                novelId,
                "chapter-" + chapterId);
    }
}

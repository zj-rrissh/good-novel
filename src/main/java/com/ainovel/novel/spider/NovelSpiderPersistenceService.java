package com.ainovel.novel.spider;

import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.service.support.NovelDomainSupport;
import com.ainovel.persistence.support.DelimitedValueCodec;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovelSpiderPersistenceService {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final NovelDomainSupport novelDomainSupport;

    public NovelSpiderPersistenceService(NovelMapper novelMapper,
                                         ChapterMapper chapterMapper,
                                         NovelDomainSupport novelDomainSupport) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.novelDomainSupport = novelDomainSupport;
    }

    @Transactional
    public PersistedNovelImport persist(CrawledNovel crawledNovel) {
        if (crawledNovel.chapters() == null || crawledNovel.chapters().isEmpty()) {
            throw new IllegalArgumentException("crawled novel has no chapters: " + crawledNovel.sourceUrl());
        }

        NovelEntity existingNovel = novelMapper.findByAuthorIdAndTitle(crawledNovel.authorId(), crawledNovel.title());
        if (existingNovel != null && existingNovel.getStatus() == NovelStatus.BANNED) {
            throw new IllegalStateException("novel is banned and cannot be overwritten by spider import: " + existingNovel.getId());
        }
        NovelEntity targetNovel = existingNovel == null ? createNovel(crawledNovel) : updateNovel(existingNovel, crawledNovel);

        Map<Integer, ChapterEntity> existingChapters = chapterMapper.findByNovelId(targetNovel.getId()).stream()
                .collect(Collectors.toMap(ChapterEntity::getChapterNo, Function.identity(), (left, right) -> left));

        int createdChapters = 0;
        int updatedChapters = 0;
        LocalDateTime importedAt = LocalDateTime.now();
        for (CrawledNovel.CrawledChapter chapter : crawledNovel.chapters()) {
            ChapterEntity existingChapter = existingChapters.get(chapter.chapterNo());
            if (existingChapter == null) {
                ChapterEntity entity = new ChapterEntity();
                entity.setNovelId(targetNovel.getId());
                entity.setChapterNo(chapter.chapterNo());
                entity.setTitle(chapter.title());
                entity.setContent(chapter.content());
                entity.setStatus(ChapterStatus.PUBLISHED);
                entity.setAuditTaskId(null);
                entity.setPublishedAt(importedAt);
                chapterMapper.insert(entity);
                createdChapters++;
                continue;
            }

            existingChapter.setChapterNo(chapter.chapterNo());
            existingChapter.setTitle(chapter.title());
            existingChapter.setContent(chapter.content());
            existingChapter.setStatus(ChapterStatus.PUBLISHED);
            existingChapter.setAuditTaskId(null);
            existingChapter.setPublishedAt(existingChapter.getPublishedAt() == null ? importedAt : existingChapter.getPublishedAt());
            chapterMapper.updateImported(existingChapter);
            updatedChapters++;
        }

        novelDomainSupport.refreshNovelStatistics(targetNovel.getId());
        novelMapper.updateStatus(targetNovel.getId(), crawledNovel.novelStatus(), null);
        novelDomainSupport.invalidateNovelCaches(targetNovel.getId());
        NovelEntity storedNovel = novelMapper.findById(targetNovel.getId());
        return new PersistedNovelImport(
                storedNovel.getId(),
                storedNovel.getTitle(),
                createdChapters,
                updatedChapters,
                crawledNovel.chapters().size(),
                storedNovel.getStatus());
    }

    private NovelEntity createNovel(CrawledNovel crawledNovel) {
        NovelEntity entity = buildNovelEntity(null, crawledNovel);
        entity.setLatestChapterId(null);
        entity.setWordCount(0L);
        entity.setAuditTaskId(null);
        novelMapper.insert(entity);
        return entity;
    }

    private NovelEntity updateNovel(NovelEntity existingNovel, CrawledNovel crawledNovel) {
        NovelEntity entity = buildNovelEntity(existingNovel.getId(), crawledNovel);
        entity.setLatestChapterId(existingNovel.getLatestChapterId());
        entity.setWordCount(existingNovel.getWordCount());
        entity.setAuditTaskId(null);
        novelMapper.updateImportedMetadata(entity);
        return entity;
    }

    private NovelEntity buildNovelEntity(Long novelId, CrawledNovel crawledNovel) {
        NovelEntity entity = new NovelEntity();
        entity.setId(novelId);
        entity.setAuthorId(crawledNovel.authorId());
        entity.setTitle(crawledNovel.title());
        entity.setIntro(crawledNovel.intro());
        entity.setCoverUrl(crawledNovel.coverUrl());
        entity.setCategoryId(crawledNovel.categoryId());
        entity.setTagIds(DelimitedValueCodec.formatLongSet(crawledNovel.tagIds()));
        entity.setStatus(crawledNovel.novelStatus());
        return entity;
    }

    public record PersistedNovelImport(
            Long novelId,
            String title,
            int createdChapters,
            int updatedChapters,
            int totalChapters,
            NovelStatus status) {
    }
}

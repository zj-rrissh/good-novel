package com.ainovel.novel.spider;

import com.ainovel.novel.domain.NovelStatus;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

public record NovelSpiderImportManifest(List<NovelSpiderNovelSpec> novels) {

    public record NovelSpiderNovelSpec(
            Long authorId,
            String detailUrl,
            String titleSelector,
            String introSelector,
            String coverSelector,
            String coverAttr,
            Long categoryId,
            Set<Long> tagIds,
            String chapterLinkSelector,
            String chapterLinkAttr,
            ChapterOrder chapterOrder,
            Integer maxChapters,
            String chapterTitleSelector,
            String chapterContentSelector,
            NovelStatus novelStatus) {

        public String resolvedCoverAttr() {
            return StringUtils.hasText(coverAttr) ? coverAttr : "src";
        }

        public String resolvedChapterLinkAttr() {
            return StringUtils.hasText(chapterLinkAttr) ? chapterLinkAttr : "href";
        }

        public ChapterOrder resolvedChapterOrder() {
            return chapterOrder == null ? ChapterOrder.ASC : chapterOrder;
        }

        public NovelStatus resolvedNovelStatus() {
            return novelStatus == null ? NovelStatus.ON_SHELF : novelStatus;
        }
    }

    public enum ChapterOrder {
        ASC,
        DESC
    }
}

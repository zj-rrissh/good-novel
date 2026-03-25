package com.ainovel.novel.spider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NovelSpiderImportServiceTests {

    @Autowired
    private NovelSpiderImportService importService;

    @Autowired
    private NovelMapper novelMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @MockBean
    private SpiderPageFetcher spiderPageFetcher;

    @Test
    void shouldImportAndUpdateNovelFromCrawledPages() throws Exception {
        String detailUrl = "https://source.test/book/100";
        String chapterOneUrl = "https://source.test/book/100/chapter-1";
        String chapterTwoUrl = "https://source.test/book/100/chapter-2";

        AtomicReference<String> detailHtml = new AtomicReference<>("""
                <html><body>
                <h1 class='title'>测试小说</h1>
                <div class='intro'>这是一本用于导入测试的小说。</div>
                <img class='cover' src='https://cdn.test/cover.png' />
                <div class='chapters'>
                  <a href='/book/100/chapter-1'>第一章 开端</a>
                  <a href='/book/100/chapter-2'>第二章 继续</a>
                </div>
                </body></html>
                """);
        AtomicReference<String> chapterOneHtml = new AtomicReference<>("""
                <html><body>
                <h1 class='chapter-title'>第一章 开端</h1>
                <div id='content'><p>第一段</p><p>第二段</p></div>
                </body></html>
                """);
        AtomicReference<String> chapterTwoHtml = new AtomicReference<>("""
                <html><body>
                <h1 class='chapter-title'>第二章 继续</h1>
                <div id='content'>第三段<br/>第四段</div>
                </body></html>
                """);

        when(spiderPageFetcher.fetch(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            return switch (url) {
                case detailUrl -> parse(detailHtml.get(), detailUrl);
                case chapterOneUrl -> parse(chapterOneHtml.get(), chapterOneUrl);
                case chapterTwoUrl -> parse(chapterTwoHtml.get(), chapterTwoUrl);
                default -> throw new IllegalArgumentException("unexpected url: " + url);
            };
        });

        NovelSpiderImportManifest manifest = new NovelSpiderImportManifest(List.of(
                new NovelSpiderImportManifest.NovelSpiderNovelSpec(
                        1002L,
                        detailUrl,
                        ".title",
                        ".intro",
                        ".cover",
                        "src",
                        9L,
                        Set.of(7L, 8L),
                        ".chapters a",
                        "href",
                        NovelSpiderImportManifest.ChapterOrder.ASC,
                        null,
                        ".chapter-title",
                        "#content",
                        NovelStatus.ON_SHELF)));

        NovelSpiderImportService.NovelSpiderImportSummary firstImport = importService.importFromManifest(manifest);
        assertEquals(1, firstImport.importedNovelCount());
        assertEquals(2, firstImport.importedChapterCount());
        assertTrue(firstImport.errors().isEmpty());

        chapterOneHtml.set("""
                <html><body>
                <h1 class='chapter-title'>第一章 开端</h1>
                <div id='content'><p>第一段修订版</p><p>第二段</p></div>
                </body></html>
                """);
        NovelSpiderImportService.NovelSpiderImportSummary secondImport = importService.importFromManifest(manifest);
        assertEquals(1, secondImport.importedNovelCount());

        NovelEntity novel = novelMapper.findByAuthorIdAndTitle(1002L, "测试小说");
        assertNotNull(novel);
        assertEquals(NovelStatus.ON_SHELF, novel.getStatus());
        assertEquals(9L, novel.getCategoryId());
        assertEquals("7,8", novel.getTagIds());
        assertNotNull(novel.getLatestChapterId());

        List<ChapterEntity> chapters = chapterMapper.findByNovelId(novel.getId());
        assertEquals(2, chapters.size());
        assertTrue(chapters.stream().allMatch(chapter -> chapter.getStatus() == ChapterStatus.PUBLISHED));
        assertTrue(chapters.stream().anyMatch(chapter -> chapter.getContent().contains("第一段修订版")));

        long expectedWordCount = chapters.stream()
                .map(ChapterEntity::getContent)
                .mapToLong(String::length)
                .sum();
        assertEquals(expectedWordCount, novel.getWordCount());
        assertEquals(chapters.get(1).getId(), novel.getLatestChapterId());
    }

    private Document parse(String html, String url) {
        return Jsoup.parse(html, url);
    }
}

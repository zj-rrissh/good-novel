package com.ainovel.novel.spider;

import com.ainovel.novel.spider.CrawledNovel.CrawledChapter;
import com.ainovel.novel.spider.NovelSpiderImportManifest.ChapterOrder;
import com.ainovel.novel.spider.NovelSpiderImportManifest.NovelSpiderNovelSpec;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NovelSpiderCrawler {

    private final SpiderPageFetcher pageFetcher;

    public NovelSpiderCrawler(SpiderPageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public CrawledNovel crawl(NovelSpiderNovelSpec spec, Long authorId) throws IOException {
        validateSpec(spec, authorId);
        Document detailDocument = pageFetcher.fetch(spec.detailUrl());

        String title = limitLength(selectRequiredText(detailDocument, spec.titleSelector(), "title"), 200);
        String intro = selectOptionalText(detailDocument, spec.introSelector());
        String coverUrl = selectOptionalAttr(detailDocument, spec.coverSelector(), spec.resolvedCoverAttr());

        List<ChapterLink> chapterLinks = collectChapterLinks(detailDocument, spec);
        if (chapterLinks.isEmpty()) {
            throw new IllegalArgumentException("chapter links not found for " + spec.detailUrl());
        }
        if (spec.resolvedChapterOrder() == ChapterOrder.DESC) {
            chapterLinks = reverse(chapterLinks);
        }
        if (spec.maxChapters() != null && spec.maxChapters() > 0 && chapterLinks.size() > spec.maxChapters()) {
            chapterLinks = new ArrayList<>(chapterLinks.subList(0, spec.maxChapters()));
        }

        List<CrawledChapter> chapters = new ArrayList<>(chapterLinks.size());
        for (int index = 0; index < chapterLinks.size(); index++) {
            ChapterLink chapterLink = chapterLinks.get(index);
            Document chapterDocument = pageFetcher.fetch(chapterLink.url());
            String chapterTitle = selectOptionalText(chapterDocument, spec.chapterTitleSelector());
            if (!StringUtils.hasText(chapterTitle)) {
                chapterTitle = chapterLink.label();
            }
            if (!StringUtils.hasText(chapterTitle)) {
                chapterTitle = "Chapter " + (index + 1);
            }
            String content = extractChapterContent(chapterDocument, spec.chapterContentSelector());
            chapters.add(new CrawledChapter(
                    index + 1,
                    limitLength(chapterTitle.trim(), 200),
                    content,
                    chapterLink.url()));
        }

        return new CrawledNovel(
                authorId,
                title,
                intro,
                coverUrl,
                spec.categoryId(),
                spec.tagIds() == null ? Set.of() : Set.copyOf(spec.tagIds()),
                spec.resolvedNovelStatus(),
                List.copyOf(chapters),
                spec.detailUrl());
    }

    private void validateSpec(NovelSpiderNovelSpec spec, Long authorId) {
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("authorId must be configured for spider import");
        }
        if (!StringUtils.hasText(spec.detailUrl())) {
            throw new IllegalArgumentException("detailUrl is required");
        }
        if (!StringUtils.hasText(spec.titleSelector())) {
            throw new IllegalArgumentException("titleSelector is required");
        }
        if (!StringUtils.hasText(spec.chapterLinkSelector())) {
            throw new IllegalArgumentException("chapterLinkSelector is required");
        }
        if (!StringUtils.hasText(spec.chapterContentSelector())) {
            throw new IllegalArgumentException("chapterContentSelector is required");
        }
    }

    private List<ChapterLink> collectChapterLinks(Document detailDocument, NovelSpiderNovelSpec spec) {
        Map<String, ChapterLink> links = new LinkedHashMap<>();
        for (Element element : detailDocument.select(spec.chapterLinkSelector())) {
            String rawUrl = element.absUrl(spec.resolvedChapterLinkAttr());
            if (!StringUtils.hasText(rawUrl)) {
                rawUrl = resolveUrl(detailDocument.baseUri(), element.attr(spec.resolvedChapterLinkAttr()));
            }
            if (!StringUtils.hasText(rawUrl)) {
                continue;
            }
            String label = normalizeInlineText(element.text());
            links.putIfAbsent(rawUrl, new ChapterLink(rawUrl, limitLength(label, 200)));
        }
        return new ArrayList<>(links.values());
    }

    private String extractChapterContent(Document document, String selector) {
        Element contentElement = selectRequiredElement(document, selector, "chapter content");
        Element normalized = contentElement.clone();
        normalized.select("script,style").remove();
        normalized.select("br").forEach(element -> element.after("\n"));
        normalized.select("p").forEach(element -> {
            element.prepend("\n");
            element.append("\n");
        });
        String text = normalizeBlockText(normalized.wholeText());
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("chapter content is empty for " + document.baseUri());
        }
        return text;
    }

    private String selectRequiredText(Document document, String selector, String fieldName) {
        return normalizeInlineText(selectRequiredElement(document, selector, fieldName).text());
    }

    private String selectOptionalText(Document document, String selector) {
        if (!StringUtils.hasText(selector)) {
            return null;
        }
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String text = normalizeInlineText(element.text());
        return StringUtils.hasText(text) ? text : null;
    }

    private String selectOptionalAttr(Document document, String selector, String attr) {
        if (!StringUtils.hasText(selector)) {
            return null;
        }
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String value = element.absUrl(attr);
        if (!StringUtils.hasText(value)) {
            value = element.attr(attr);
        }
        value = normalizeInlineText(value);
        return StringUtils.hasText(value) ? value : null;
    }

    private Element selectRequiredElement(Document document, String selector, String fieldName) {
        Element element = document.selectFirst(selector);
        if (element == null) {
            throw new IllegalArgumentException(fieldName + " selector not found: " + selector);
        }
        return element;
    }

    private List<ChapterLink> reverse(List<ChapterLink> chapterLinks) {
        List<ChapterLink> reversed = new ArrayList<>(chapterLinks);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private String normalizeInlineText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeBlockText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String[] lines = value.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> normalizedLines = new ArrayList<>(lines.length);
        boolean previousBlank = false;
        for (String line : lines) {
            String normalizedLine = normalizeInlineText(line);
            if (!StringUtils.hasText(normalizedLine)) {
                if (!previousBlank && !normalizedLines.isEmpty()) {
                    normalizedLines.add("");
                }
                previousBlank = true;
                continue;
            }
            normalizedLines.add(normalizedLine);
            previousBlank = false;
        }
        while (!normalizedLines.isEmpty() && normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
            normalizedLines.remove(normalizedLines.size() - 1);
        }
        return String.join("\n", normalizedLines).trim();
    }

    private String resolveUrl(String baseUri, String link) {
        if (!StringUtils.hasText(link)) {
            return null;
        }
        try {
            if (!StringUtils.hasText(baseUri)) {
                return link;
            }
            return URI.create(baseUri).resolve(link).toString();
        } catch (IllegalArgumentException ex) {
            return link;
        }
    }

    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ChapterLink(String url, String label) {
    }
}

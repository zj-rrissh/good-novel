package com.ainovel.novel.spider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NovelSpiderImportService {

    private final ObjectMapper objectMapper;
    private final NovelSpiderProperties properties;
    private final NovelSpiderCrawler crawler;
    private final NovelSpiderPersistenceService persistenceService;

    public NovelSpiderImportService(ObjectMapper objectMapper,
                                    NovelSpiderProperties properties,
                                    NovelSpiderCrawler crawler,
                                    NovelSpiderPersistenceService persistenceService) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.crawler = crawler;
        this.persistenceService = persistenceService;
    }

    public NovelSpiderImportSummary importFromSpecFile(Path specFile) throws IOException {
        NovelSpiderImportManifest manifest = objectMapper.readValue(specFile.toFile(), NovelSpiderImportManifest.class);
        return importFromManifest(manifest);
    }

    public NovelSpiderImportSummary importFromManifest(NovelSpiderImportManifest manifest) {
        if (manifest == null || manifest.novels() == null || manifest.novels().isEmpty()) {
            throw new IllegalArgumentException("novel spider manifest must contain at least one novel");
        }

        List<NovelSpiderPersistenceService.PersistedNovelImport> imports = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int importedChapterCount = 0;

        for (NovelSpiderImportManifest.NovelSpiderNovelSpec spec : manifest.novels()) {
            try {
                Long authorId = resolveAuthorId(spec.authorId());
                CrawledNovel crawledNovel = crawler.crawl(spec, authorId);
                NovelSpiderPersistenceService.PersistedNovelImport result = persistenceService.persist(crawledNovel);
                imports.add(result);
                importedChapterCount += result.totalChapters();
            } catch (Exception ex) {
                String source = spec == null ? "<unknown>" : spec.detailUrl();
                String message = "failed to import novel from " + source + ": " + ex.getMessage();
                if (!properties.isContinueOnError()) {
                    throw new IllegalStateException(message, ex);
                }
                errors.add(message);
            }
        }

        return new NovelSpiderImportSummary(List.copyOf(imports), List.copyOf(errors), importedChapterCount);
    }

    private Long resolveAuthorId(Long authorId) {
        if (authorId != null && authorId > 0) {
            return authorId;
        }
        Long defaultAuthorId = properties.getDefaultAuthorId();
        if (defaultAuthorId == null || defaultAuthorId <= 0) {
            throw new IllegalArgumentException("default authorId is not configured");
        }
        return defaultAuthorId;
    }

    public record NovelSpiderImportSummary(
            List<NovelSpiderPersistenceService.PersistedNovelImport> novels,
            List<String> errors,
            int importedChapterCount) {

        public int importedNovelCount() {
            return novels.size();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}

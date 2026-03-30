package com.ainovel.novel.spider;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "ainovel.spider", name = "enabled", havingValue = "true")
public class NovelSpiderImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NovelSpiderImportRunner.class);

    private final NovelSpiderProperties properties;
    private final NovelSpiderImportService importService;

    public NovelSpiderImportRunner(NovelSpiderProperties properties, NovelSpiderImportService importService) {
        this.properties = properties;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!StringUtils.hasText(properties.getSpecFile())) {
            throw new IllegalStateException("ainovel.spider.spec-file must be set when spider import is enabled");
        }

        NovelSpiderImportService.NovelSpiderImportSummary summary =
                importService.importFromSpecFile(Path.of(properties.getSpecFile()));

        log.info("novel spider import finished: novels={}, chapters={}, errors={}",
                summary.importedNovelCount(),
                summary.importedChapterCount(),
                summary.errors().size());
        summary.novels().forEach(item -> log.info(
                "imported novel id={}, title={}, createdChapters={}, updatedChapters={}, status={}",
                item.novelId(),
                item.title(),
                item.createdChapters(),
                item.updatedChapters(),
                item.status()));
        if (summary.hasErrors()) {
            summary.errors().forEach(error -> log.warn("novel spider import warning: {}", error));
        }
    }
}

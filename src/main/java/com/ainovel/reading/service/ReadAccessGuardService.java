package com.ainovel.reading.service;

public interface ReadAccessGuardService {

    void verifyNovelVisible(Long novelId);

    void verifyChapterVisible(Long chapterId);
}

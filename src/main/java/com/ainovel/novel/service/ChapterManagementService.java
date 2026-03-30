package com.ainovel.novel.service;

import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.UpdateChapterRequest;
import com.ainovel.novel.vo.NovelChapterVO;

public interface ChapterManagementService {

    NovelChapterVO createChapter(Long novelId, CreateChapterRequest request, String idempotencyKey);

    NovelChapterVO updateChapter(Long chapterId, UpdateChapterRequest request);
}

package com.ainovel.novel.service;

import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.UpdateNovelRequest;
import com.ainovel.novel.vo.NovelDetailVO;

public interface NovelDraftService {

    NovelDetailVO createNovel(CreateNovelRequest request, String idempotencyKey);

    NovelDetailVO updateNovel(Long novelId, UpdateNovelRequest request);
}

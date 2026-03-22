package com.ainovel.reading.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.reading.vo.ChapterMetaVO;
import com.ainovel.reading.vo.ReadingNovelDetailVO;

public interface NovelReadService {

    ReadingNovelDetailVO getNovelDetail(Long novelId);

    PageResponse<ChapterMetaVO> getChapterPage(Long novelId, int page, int size);
}

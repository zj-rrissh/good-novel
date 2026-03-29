package com.ainovel.reading.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.reading.vo.ReadingHistoryVO;

public interface ReadingHistoryService {

    void recordHistory(Long userId, Long novelId, Long chapterId);

    void deleteHistory(Long userId, Long novelId);

    PageResponse<ReadingHistoryVO> listHistory(Long userId, int page, int size);
}

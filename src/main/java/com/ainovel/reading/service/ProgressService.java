package com.ainovel.reading.service;

import com.ainovel.reading.dto.UpdateReadingProgressRequest;
import com.ainovel.reading.vo.ReadingProgressVO;

public interface ProgressService {

    ReadingProgressVO saveProgress(Long userId, UpdateReadingProgressRequest request, String idempotencyKey);

    ReadingProgressVO getProgress(Long userId, Long novelId);
}

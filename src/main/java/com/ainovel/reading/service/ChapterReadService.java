package com.ainovel.reading.service;

import com.ainovel.reading.vo.ChapterContentVO;

public interface ChapterReadService {

    ChapterContentVO getChapterContent(Long chapterId);
}

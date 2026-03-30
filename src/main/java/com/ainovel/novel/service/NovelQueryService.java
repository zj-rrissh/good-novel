package com.ainovel.novel.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.novel.dto.AdminNovelQuery;
import com.ainovel.novel.vo.NovelSummaryVO;

public interface NovelQueryService {

    PageResponse<NovelSummaryVO> queryAdminNovels(AdminNovelQuery query);
}

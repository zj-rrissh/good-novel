package com.ainovel.novel.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.novel.dto.AdminNovelQuery;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.vo.NovelSummaryVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NovelQueryServiceImpl implements NovelQueryService {

    private final NovelMapper novelMapper;

    public NovelQueryServiceImpl(NovelMapper novelMapper) {
        this.novelMapper = novelMapper;
    }

    @Override
    public PageResponse<NovelSummaryVO> queryAdminNovels(AdminNovelQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        int offset = (page - 1) * size;
        String keyword = normalizeKeyword(query.keyword());

        long total = novelMapper.countAdminQuery(query.status(), query.authorId(), query.categoryId(), keyword);
        if (total == 0) {
            return PageResponse.of(List.of(), 0, page, size);
        }

        List<NovelSummaryVO> records = novelMapper.queryAdmin(
                        query.status(),
                        query.authorId(),
                        query.categoryId(),
                        keyword,
                        offset,
                        size)
                .stream()
                .map(this::toSummary)
                .toList();

        return PageResponse.of(records, total, page, size);
    }

    private NovelSummaryVO toSummary(NovelEntity entity) {
        return new NovelSummaryVO(
                entity.getId(),
                entity.getTitle(),
                entity.getCoverUrl(),
                entity.getAuthorId(),
                entity.getStatus());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

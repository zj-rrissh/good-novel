package com.ainovel.novel.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.service.support.NovelDomainSupport;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovelShelfServiceImpl implements NovelShelfService {

    private final NovelMapper novelMapper;
    private final NovelDomainSupport novelDomainSupport;

    public NovelShelfServiceImpl(NovelMapper novelMapper, NovelDomainSupport novelDomainSupport) {
        this.novelMapper = novelMapper;
        this.novelDomainSupport = novelDomainSupport;
    }

    @Override
    @Transactional
    public void onShelf(Long novelId, String reason) {
        NovelEntity novel = novelDomainSupport.requireOwnedNovel(novelId);
        int updated = novelMapper.updateStatus(novelId, NovelStatus.ON_SHELF, List.of(NovelStatus.PUBLISHED, NovelStatus.OFF_SHELF));
        if (updated == 0) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "novel status does not allow on shelf");
        }
        novelDomainSupport.invalidateNovelCaches(novelId);
    }

    @Override
    @Transactional
    public void offShelf(Long novelId, String reason) {
        novelDomainSupport.requireOwnedNovel(novelId);
        int updated = novelMapper.updateStatus(novelId, NovelStatus.OFF_SHELF, List.of(NovelStatus.ON_SHELF, NovelStatus.PUBLISHED));
        if (updated == 0) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "novel status does not allow off shelf");
        }
        novelDomainSupport.invalidateNovelCaches(novelId);
    }

    @Override
    @Transactional
    public void ban(Long novelId, String reason) {
        novelDomainSupport.requireNovel(novelId);
        novelMapper.updateStatus(novelId, NovelStatus.BANNED, List.of(
                NovelStatus.DRAFT,
                NovelStatus.PENDING_AUDIT,
                NovelStatus.REJECTED,
                NovelStatus.PUBLISHED,
                NovelStatus.ON_SHELF,
                NovelStatus.OFF_SHELF));
        novelDomainSupport.invalidateNovelCaches(novelId);
    }
}

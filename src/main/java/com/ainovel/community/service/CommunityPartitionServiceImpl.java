package com.ainovel.community.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.mapper.CommunityPartitionMapper;
import com.ainovel.community.vo.CommunityPartitionVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.mapper.NovelMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityPartitionServiceImpl implements CommunityPartitionService {

    private final CommunityPartitionMapper communityPartitionMapper;
    private final NovelMapper novelMapper;

    public CommunityPartitionServiceImpl(CommunityPartitionMapper communityPartitionMapper,
                                         NovelMapper novelMapper) {
        this.communityPartitionMapper = communityPartitionMapper;
        this.novelMapper = novelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityPartitionVO> listNovelPartitions(Long novelId) {
        if (novelMapper.findById(novelId) == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
        }
        return communityPartitionMapper.queryActiveByNovelId(novelId).stream()
                .map(partition -> new CommunityPartitionVO(
                        partition.getId(),
                        partition.getNovelId(),
                        partition.getPartitionKey(),
                        partition.getPartitionName(),
                        partition.getSortOrder()))
                .toList();
    }
}

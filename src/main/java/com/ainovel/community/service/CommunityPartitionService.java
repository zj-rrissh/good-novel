package com.ainovel.community.service;

import com.ainovel.community.vo.CommunityPartitionVO;
import java.util.List;

public interface CommunityPartitionService {

    List<CommunityPartitionVO> listNovelPartitions(Long novelId);
}

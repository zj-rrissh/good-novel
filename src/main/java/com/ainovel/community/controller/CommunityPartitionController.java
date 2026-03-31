package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.community.service.CommunityPartitionService;
import com.ainovel.community.vo.CommunityPartitionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1)
public class CommunityPartitionController {

    private final CommunityPartitionService communityPartitionService;

    public CommunityPartitionController(CommunityPartitionService communityPartitionService) {
        this.communityPartitionService = communityPartitionService;
    }

    @GetMapping("/novels/{novelId}/community/partitions")
    public Result<List<CommunityPartitionVO>> listNovelPartitions(@PathVariable Long novelId) {
        return Result.success(communityPartitionService.listNovelPartitions(novelId));
    }
}

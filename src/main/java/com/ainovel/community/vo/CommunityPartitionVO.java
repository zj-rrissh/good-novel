package com.ainovel.community.vo;

public record CommunityPartitionVO(
        Long partitionId,
        Long novelId,
        String partitionKey,
        String partitionName,
        Integer sortOrder) {
}

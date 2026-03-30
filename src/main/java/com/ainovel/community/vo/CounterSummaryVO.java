package com.ainovel.community.vo;

public record CounterSummaryVO(
        String targetType,
        Long targetId,
        long commentCount,
        long likeCount,
        long favoriteCount,
        long followerCount,
        boolean liked,
        boolean favorited,
        boolean followed) {
}

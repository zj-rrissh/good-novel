package com.ainovel.user.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.user.dto.DeliverMessageRequest;
import com.ainovel.user.dto.MarkMessagesReadRequest;
import com.ainovel.user.vo.UserMessageVO;

public interface UserMessageService {

    PageResponse<UserMessageVO> queryMessages(Long userId, int page, int size, String type, Boolean readStatus);

    void markRead(Long userId, MarkMessagesReadRequest request);

    void deliver(String idempotencyKey, DeliverMessageRequest request);
}

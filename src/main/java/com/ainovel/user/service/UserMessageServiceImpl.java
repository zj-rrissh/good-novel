package com.ainovel.user.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.user.domain.MessageType;
import com.ainovel.user.dto.DeliverMessageRequest;
import com.ainovel.user.dto.MarkMessagesReadRequest;
import com.ainovel.user.entity.UserMessageEntity;
import com.ainovel.user.mapper.UserMessageMapper;
import com.ainovel.user.vo.UserMessageVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserMessageServiceImpl implements UserMessageService {

    private final UserMessageMapper userMessageMapper;

    public UserMessageServiceImpl(UserMessageMapper userMessageMapper) {
        this.userMessageMapper = userMessageMapper;
    }

    @Override
    public PageResponse<UserMessageVO> queryMessages(Long userId, int page, int size, String type, Boolean readStatus) {
        MessageType messageType = parseType(type);
        int offset = (page - 1) * size;
        long total = userMessageMapper.countByUser(userId, messageType, readStatus);
        List<UserMessageVO> records = userMessageMapper.queryByUser(userId, messageType, readStatus, offset, size)
                .stream()
                .map(this::toVO)
                .toList();
        return PageResponse.of(records, total, page, size);
    }

    @Override
    @Transactional
    public void markRead(Long userId, MarkMessagesReadRequest request) {
        userMessageMapper.markRead(userId, request.messageIds());
    }

    @Override
    @Transactional
    public void deliver(String idempotencyKey, DeliverMessageRequest request) {
        UserMessageEntity entity = new UserMessageEntity();
        entity.setToUserId(request.toUserId());
        entity.setType(request.type());
        entity.setTitle(request.title().trim());
        entity.setContent(request.content().trim());
        entity.setBizType(request.bizType().trim());
        entity.setBizId(request.bizId());
        entity.setProducer(request.producer().trim());
        entity.setTraceId(request.traceId().trim());
        userMessageMapper.insert(entity);
    }

    @Override
    public long countUnread(Long userId) {
        return userMessageMapper.countUnread(userId);
    }

    private MessageType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return MessageType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "unsupported message type");
        }
    }

    private UserMessageVO toVO(UserMessageEntity entity) {
        return new UserMessageVO(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getReadAt() != null,
                entity.getCreatedAt());
    }
}

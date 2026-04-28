package com.ainovel.admin.service;

import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.admin.dto.AdminOperationLogQuery;
import com.ainovel.admin.entity.AdminOperationLogEntity;
import com.ainovel.admin.mapper.AdminOperationLogMapper;
import com.ainovel.admin.vo.AdminOperationLogVO;
import com.ainovel.common.api.PageResponse;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.infrastructure.log.TraceIdFilter;
import com.ainovel.infrastructure.log.TraceIdHolder;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminOperationLogServiceImpl(AdminOperationLogMapper adminOperationLogMapper) {
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public void record(AuditAction action, String bizType, Long bizId, String fromStatus, String toStatus, String reason) {
        AdminOperationLogEntity entity = new AdminOperationLogEntity();
        entity.setAction(action == null ? null : action.name());
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setReason(reason);
        entity.setOperatorId(CurrentUserHolder.get().map(CurrentUser::userId).orElse(null));
        entity.setOperatorRoles(resolveOperatorRoles());
        entity.setTraceId(resolveTraceId());
        entity.setRequestPath(resolveRequestPath());
        adminOperationLogMapper.insert(entity);
    }

    @Override
    public PageResponse<AdminOperationLogVO> query(AdminOperationLogQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        int offset = (page - 1) * size;
        long total = adminOperationLogMapper.countQuery(
                query.action(),
                query.bizType(),
                query.bizId(),
                query.operatorId());
        return PageResponse.of(
                adminOperationLogMapper.query(
                                query.action(),
                                query.bizType(),
                                query.bizId(),
                                query.operatorId(),
                                offset,
                                size)
                        .stream()
                        .map(this::toVO)
                        .toList(),
                total,
                page,
                size);
    }

    private AdminOperationLogVO toVO(AdminOperationLogEntity entity) {
        return new AdminOperationLogVO(
                entity.getLogId(),
                entity.getAction(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getOperatorId(),
                entity.getOperatorRoles(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getReason(),
                entity.getTraceId(),
                entity.getRequestPath(),
                entity.getCreatedAt());
    }

    private String resolveOperatorRoles() {
        return CurrentUserHolder.get()
                .map(CurrentUser::roles)
                .stream()
                .flatMap(set -> set.stream()
                        .filter(Objects::nonNull)
                        .map(Enum::name))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }

    private String resolveTraceId() {
        String traceId = TraceIdHolder.get().orElse(null);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String requestTraceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (requestTraceId != null && !requestTraceId.isBlank()) {
            return requestTraceId;
        }
        String requestId = request.getHeader(RequestHeaders.X_REQUEST_ID);
        return requestId == null || requestId.isBlank() ? null : requestId;
    }

    private String resolveRequestPath() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }
}

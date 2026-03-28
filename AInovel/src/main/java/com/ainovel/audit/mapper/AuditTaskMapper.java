package com.ainovel.audit.mapper;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.BizType;
import com.ainovel.audit.domain.RiskLevel;
import com.ainovel.audit.entity.AuditTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AuditTaskMapper {

    @Insert("""
            insert into audit_task (
                biz_type, biz_id, content_snapshot, content_hash, audit_status, risk_level, reason_code, reason_text,
                reviewer_id, retry_count, rule_version, reviewed_at
            ) values (
                #{bizType}, #{bizId}, #{contentSnapshot}, #{contentHash}, #{auditStatus}, #{riskLevel}, #{reasonCode},
                #{reasonText}, #{reviewerId}, #{retryCount}, #{ruleVersion}, #{reviewedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "taskId")
    int insert(AuditTaskEntity entity);

    @Select("""
            select task_id, biz_type, biz_id, content_snapshot, content_hash, audit_status, risk_level, reason_code,
                   reason_text, reviewer_id, retry_count, rule_version, reviewed_at, created_at, updated_at
            from audit_task
            where task_id = #{taskId}
            limit 1
            """)
    AuditTaskEntity findById(Long taskId);

    @Select("""
            select task_id, biz_type, biz_id, content_snapshot, content_hash, audit_status, risk_level, reason_code,
                   reason_text, reviewer_id, retry_count, rule_version, reviewed_at, created_at, updated_at
            from audit_task
            where biz_type = #{bizType}
              and biz_id = #{bizId}
              and content_hash = #{contentHash}
            order by task_id desc
            limit 1
            """)
    AuditTaskEntity findLatestByBizHash(@Param("bizType") BizType bizType,
                                        @Param("bizId") Long bizId,
                                        @Param("contentHash") String contentHash);

    @Select("""
            <script>
            select count(1)
            from audit_task
            where 1 = 1
            <if test='status != null and status != ""'>
                and audit_status = #{status}
            </if>
            <if test='bizType != null and bizType != ""'>
                and biz_type = #{bizType}
            </if>
            <if test='riskLevel != null and riskLevel != ""'>
                and risk_level = #{riskLevel}
            </if>
            </script>
            """)
    long countQuery(@Param("status") String status,
                    @Param("bizType") String bizType,
                    @Param("riskLevel") String riskLevel);

    @Select("""
            <script>
            select task_id, biz_type, biz_id, content_snapshot, content_hash, audit_status, risk_level, reason_code,
                   reason_text, reviewer_id, retry_count, rule_version, reviewed_at, created_at, updated_at
            from audit_task
            where 1 = 1
            <if test='status != null and status != ""'>
                and audit_status = #{status}
            </if>
            <if test='bizType != null and bizType != ""'>
                and biz_type = #{bizType}
            </if>
            <if test='riskLevel != null and riskLevel != ""'>
                and risk_level = #{riskLevel}
            </if>
            order by created_at desc, task_id desc
            limit #{size} offset #{offset}
            </script>
            """)
    List<AuditTaskEntity> query(@Param("status") String status,
                                @Param("bizType") String bizType,
                                @Param("riskLevel") String riskLevel,
                                @Param("offset") int offset,
                                @Param("size") int size);

    @Update("""
            update audit_task
            set audit_status = #{auditStatus},
                risk_level = #{riskLevel},
                reason_code = #{reasonCode},
                reason_text = #{reasonText},
                reviewer_id = #{reviewerId},
                reviewed_at = #{reviewedAt},
                updated_at = current_timestamp(3)
            where task_id = #{taskId}
            """)
    int review(@Param("taskId") Long taskId,
               @Param("auditStatus") AuditStatus auditStatus,
               @Param("riskLevel") RiskLevel riskLevel,
               @Param("reasonCode") String reasonCode,
               @Param("reasonText") String reasonText,
               @Param("reviewerId") Long reviewerId,
               @Param("reviewedAt") LocalDateTime reviewedAt);

    @Update("""
            update audit_task
            set audit_status = #{auditStatus},
                risk_level = #{riskLevel},
                reason_code = #{reasonCode},
                reason_text = #{reasonText},
                updated_at = current_timestamp(3)
            where task_id = #{taskId}
            """)
    int updateExecutionResult(@Param("taskId") Long taskId,
                              @Param("auditStatus") AuditStatus auditStatus,
                              @Param("riskLevel") RiskLevel riskLevel,
                              @Param("reasonCode") String reasonCode,
                              @Param("reasonText") String reasonText);
}

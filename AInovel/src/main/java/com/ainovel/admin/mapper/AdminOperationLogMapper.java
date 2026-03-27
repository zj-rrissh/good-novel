package com.ainovel.admin.mapper;

import com.ainovel.admin.entity.AdminOperationLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AdminOperationLogMapper {

    @Insert("""
            insert into admin_operation_log (
                action, biz_type, biz_id, operator_id, operator_roles, from_status, to_status,
                reason, trace_id, request_path
            ) values (
                #{action}, #{bizType}, #{bizId}, #{operatorId}, #{operatorRoles}, #{fromStatus}, #{toStatus},
                #{reason}, #{traceId}, #{requestPath}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    int insert(AdminOperationLogEntity entity);

    @Select("""
            <script>
            select count(1)
            from admin_operation_log
            where 1 = 1
            <if test='action != null and action != ""'>
                and action = #{action}
            </if>
            <if test='bizType != null and bizType != ""'>
                and biz_type = #{bizType}
            </if>
            <if test='bizId != null'>
                and biz_id = #{bizId}
            </if>
            <if test='operatorId != null'>
                and operator_id = #{operatorId}
            </if>
            </script>
            """)
    long countQuery(@Param("action") String action,
                    @Param("bizType") String bizType,
                    @Param("bizId") Long bizId,
                    @Param("operatorId") Long operatorId);

    @Select("""
            <script>
            select log_id, action, biz_type, biz_id, operator_id, operator_roles, from_status, to_status,
                   reason, trace_id, request_path, created_at
            from admin_operation_log
            where 1 = 1
            <if test='action != null and action != ""'>
                and action = #{action}
            </if>
            <if test='bizType != null and bizType != ""'>
                and biz_type = #{bizType}
            </if>
            <if test='bizId != null'>
                and biz_id = #{bizId}
            </if>
            <if test='operatorId != null'>
                and operator_id = #{operatorId}
            </if>
            order by created_at desc, log_id desc
            limit #{size} offset #{offset}
            </script>
            """)
    List<AdminOperationLogEntity> query(@Param("action") String action,
                                        @Param("bizType") String bizType,
                                        @Param("bizId") Long bizId,
                                        @Param("operatorId") Long operatorId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);
}

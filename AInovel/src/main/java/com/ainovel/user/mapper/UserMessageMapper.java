package com.ainovel.user.mapper;

import com.ainovel.user.domain.MessageType;
import com.ainovel.user.entity.UserMessageEntity;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMessageMapper {

    @Insert("""
            insert into user_message (
                to_user_id, type, title, content, biz_type, biz_id, producer, trace_id, read_at
            ) values (
                #{toUserId}, #{type}, #{title}, #{content}, #{bizType}, #{bizId}, #{producer}, #{traceId}, #{readAt}
            )
            """)
    int insert(UserMessageEntity entity);

    @Select("""
            <script>
            select count(1)
            from user_message
            where to_user_id = #{userId}
            <if test='type != null'>
                and type = #{type}
            </if>
            <if test='readStatus != null'>
                <choose>
                    <when test='readStatus'>
                        and read_at is not null
                    </when>
                    <otherwise>
                        and read_at is null
                    </otherwise>
                </choose>
            </if>
            </script>
            """)
    long countByUser(@Param("userId") Long userId,
                     @Param("type") MessageType type,
                     @Param("readStatus") Boolean readStatus);

    @Select("""
            <script>
            select id, to_user_id, type, title, content, biz_type, biz_id, producer, trace_id, read_at, created_at, updated_at
            from user_message
            where to_user_id = #{userId}
            <if test='type != null'>
                and type = #{type}
            </if>
            <if test='readStatus != null'>
                <choose>
                    <when test='readStatus'>
                        and read_at is not null
                    </when>
                    <otherwise>
                        and read_at is null
                    </otherwise>
                </choose>
            </if>
            order by created_at desc
            limit #{size} offset #{offset}
            </script>
            """)
    List<UserMessageEntity> queryByUser(@Param("userId") Long userId,
                                        @Param("type") MessageType type,
                                        @Param("readStatus") Boolean readStatus,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    @Update("""
            <script>
            update user_message
            set read_at = current_timestamp(3),
                updated_at = current_timestamp(3)
            where to_user_id = #{userId}
              and read_at is null
              and id in
              <foreach collection='messageIds' item='messageId' open='(' separator=',' close=')'>
                  #{messageId}
              </foreach>
            </script>
            """)
    int markRead(@Param("userId") Long userId, @Param("messageIds") Set<Long> messageIds);
}

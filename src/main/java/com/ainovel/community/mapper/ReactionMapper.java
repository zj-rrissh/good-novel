package com.ainovel.community.mapper;

import com.ainovel.community.domain.ReactionStatus;
import com.ainovel.community.domain.ReactionType;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.entity.ReactionEntity;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactionMapper {

    @Select("""
            select id, reaction_type, target_type, target_id, user_id, status, created_at, updated_at
            from reaction
            where user_id = #{userId}
              and reaction_type = #{reactionType}
              and target_type = #{targetType}
              and target_id = #{targetId}
            limit 1
            """)
    Optional<ReactionEntity> findByUserAndTarget(@Param("userId") Long userId,
                                                 @Param("reactionType") ReactionType reactionType,
                                                 @Param("targetType") TargetType targetType,
                                                 @Param("targetId") Long targetId);

    @Insert("""
            insert into reaction (reaction_type, target_type, target_id, user_id, status)
            values (#{reactionType}, #{targetType}, #{targetId}, #{userId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReactionEntity entity);

    @Update("""
            update reaction
            set status = #{status},
                updated_at = current_timestamp(3)
            where id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") ReactionStatus status);

    @Select("""
            select count(1)
            from reaction
            where reaction_type = #{reactionType}
              and target_type = #{targetType}
              and target_id = #{targetId}
              and status = 'ACTIVE'
            """)
    long countActiveByTarget(@Param("reactionType") ReactionType reactionType,
                             @Param("targetType") TargetType targetType,
                             @Param("targetId") Long targetId);

    @Select("""
            select exists(
                select 1
                from reaction
                where user_id = #{userId}
                  and reaction_type = #{reactionType}
                  and target_type = #{targetType}
                  and target_id = #{targetId}
                  and status = 'ACTIVE'
            )
            """)
    boolean existsActiveByUserAndTarget(@Param("userId") Long userId,
                                        @Param("reactionType") ReactionType reactionType,
                                        @Param("targetType") TargetType targetType,
                                        @Param("targetId") Long targetId);
}

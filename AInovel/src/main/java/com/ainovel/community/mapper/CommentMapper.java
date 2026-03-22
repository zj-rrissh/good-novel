package com.ainovel.community.mapper;

import com.ainovel.community.domain.Comment;
import com.ainovel.community.domain.TargetType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * Comment persistence contract with optimistic locking semantics.
 *
 * <p>Implementations must guarantee that {@link #softDelete(Long, Long)} only affects one row when the
 * expectedVersion matches, so callers can rely on optimistic concurrency.</p>
 */
@Repository
public interface CommentMapper {

    /**
     * Insert a new comment. Implementation should initialize {@code version} to 0.
     */
    @Insert("""
            insert into comment (
                id, target_type, target_id, user_id, parent_id, reply_to_user_id, content, status, version, created_at
            ) values (
                #{id}, #{targetType}, #{targetId}, #{userId}, #{parentId}, #{replyToUserId}, #{content}, #{status}, #{version}, #{createdAt}
            )
            """)
    int insert(Comment comment);

    /**
     * Checks whether the same user already submitted an identical comment to the same target after the given
     * {@code threshold} time; used to short-circuit duplicate submissions in a small window.
     */
    @Select("""
            select exists(
                select 1
                from comment
                where user_id = #{userId}
                  and target_type = #{targetType}
                  and target_id = #{targetId}
                  and content = #{content}
                  and status <> 'DELETED'
                  and created_at >= #{threshold}
            )
            """)
    boolean existsRecentDuplicate(@Param("userId") Long userId,
                                  @Param("targetType") TargetType targetType,
                                  @Param("targetId") Long targetId,
                                  @Param("content") String content,
                                  @Param("threshold") LocalDateTime threshold);

    @Select("""
            select
                id,
                target_type as targetType,
                target_id as targetId,
                user_id as userId,
                parent_id as parentId,
                reply_to_user_id as replyToUserId,
                content,
                status,
                created_at as createdAt,
                version
            from comment
            where id = #{id}
            limit 1
            """)
    Optional<Comment> findById(@Param("id") Long id);

    /**
     * Soft delete with optimistic lock. Returns affected row count.
     */
    @Update("""
            update comment
            set status = 'DELETED',
                version = version + 1
            where id = #{commentId}
              and version = #{expectedVersion}
              and status <> 'DELETED'
            """)
    int softDelete(@Param("commentId") Long commentId, @Param("expectedVersion") Long expectedVersion);
}

package com.ainovel.community.mapper;

import com.ainovel.community.domain.TargetType;
import com.ainovel.community.entity.CommentEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
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
                target_type, target_id, user_id, parent_id, reply_to_user_id, content, status, version, created_at
            ) values (
                #{targetType}, #{targetId}, #{userId}, #{parentId}, #{replyToUserId}, #{content}, #{status}, #{version}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommentEntity comment);

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
                target_type,
                target_id,
                user_id,
                parent_id,
                reply_to_user_id,
                content,
                status,
                created_at,
                version
            from comment
            where id = #{id}
            limit 1
            """)
    Optional<CommentEntity> findById(@Param("id") Long id);

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

    @Select("""
            select
                id,
                target_type,
                target_id,
                user_id,
                parent_id,
                reply_to_user_id,
                content,
                status,
                created_at,
                version
            from comment
            where target_type = #{targetType}
              and target_id = #{targetId}
              and status = 'VISIBLE'
              and parent_id is null
            order by created_at desc, id desc
            limit #{size} offset #{offset}
            """)
    List<CommentEntity> queryVisibleRootByTargetOrderNew(@Param("targetType") TargetType targetType,
                                                         @Param("targetId") Long targetId,
                                                         @Param("offset") int offset,
                                                         @Param("size") int size);

    @Select("""
            select
                id,
                target_type,
                target_id,
                user_id,
                parent_id,
                reply_to_user_id,
                content,
                status,
                created_at,
                version
            from comment
            where target_type = #{targetType}
              and target_id = #{targetId}
              and status = 'VISIBLE'
              and parent_id is null
            order by created_at asc, id asc
            limit #{size} offset #{offset}
            """)
    List<CommentEntity> queryVisibleRootByTargetOrderOld(@Param("targetType") TargetType targetType,
                                                         @Param("targetId") Long targetId,
                                                         @Param("offset") int offset,
                                                         @Param("size") int size);

    @Select("""
            <script>
            select
                id,
                target_type,
                target_id,
                user_id,
                parent_id,
                reply_to_user_id,
                content,
                status,
                created_at,
                version
            from comment
            where status = 'VISIBLE'
              and parent_id in
              <foreach collection='parentIds' item='parentId' open='(' separator=',' close=')'>
                  #{parentId}
              </foreach>
            order by parent_id asc, created_at asc, id asc
            </script>
            """)
    List<CommentEntity> queryVisibleByParentIds(@Param("parentIds") List<Long> parentIds);

    @Select("""
            <script>
            select id
            from comment
            where parent_id in
            <foreach collection='parentIds' item='parentId' open='(' separator=',' close=')'>
                #{parentId}
            </foreach>
            </script>
            """)
    List<Long> queryChildIdsByParentIds(@Param("parentIds") List<Long> parentIds);

    @Update("""
            <script>
            update comment
            set status = 'HIDDEN',
                version = version + 1
            where id in
            <foreach collection='commentIds' item='commentId' open='(' separator=',' close=')'>
                #{commentId}
            </foreach>
              and status not in ('HIDDEN', 'DELETED')
            </script>
            """)
    int batchHideByIds(@Param("commentIds") List<Long> commentIds);

    @Select("""
            select count(1)
            from comment
            where target_type = #{targetType}
              and target_id = #{targetId}
              and status = 'VISIBLE'
            """)
    long countVisibleByTarget(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId);

    @Select("""
            select count(1)
            from comment
            where target_type = #{targetType}
              and target_id = #{targetId}
              and status = 'VISIBLE'
              and parent_id is null
            """)
    long countVisibleRootByTarget(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId);
}

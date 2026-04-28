package com.ainovel.community.mapper;

import com.ainovel.community.domain.FollowStatus;
import com.ainovel.community.entity.UserFollowEntity;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowMapper {

    @Select("""
            select id, user_id, target_user_id, status, created_at, updated_at
            from user_follow
            where user_id = #{userId}
              and target_user_id = #{targetUserId}
            limit 1
            """)
    Optional<UserFollowEntity> findByUserAndTarget(@Param("userId") Long userId,
                                                   @Param("targetUserId") Long targetUserId);

    @Insert("""
            insert into user_follow (user_id, target_user_id, status)
            values (#{userId}, #{targetUserId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserFollowEntity entity);

    @Update("""
            update user_follow
            set status = #{status},
                updated_at = current_timestamp(3)
            where id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") FollowStatus status);

    @Select("""
            select count(1)
            from user_follow
            where target_user_id = #{targetUserId}
              and status = 'ACTIVE'
            """)
    long countActiveFollowers(@Param("targetUserId") Long targetUserId);

    @Select("""
            select exists(
                select 1
                from user_follow
                where user_id = #{userId}
                  and target_user_id = #{targetUserId}
                  and status = 'ACTIVE'
            )
            """)
    boolean existsActiveFollow(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
}

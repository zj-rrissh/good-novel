package com.ainovel.user.mapper;

import com.ainovel.user.entity.UserProfileEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserProfileMapper {

    @Insert("""
            insert into user_profile (user_id, nickname, avatar_url, bio, level, verified_status)
            values (#{userId}, #{nickname}, #{avatarUrl}, #{bio}, #{level}, #{verifiedStatus})
            """)
    int insert(UserProfileEntity entity);

    @Select("""
            select user_id, nickname, avatar_url, bio, level, verified_status, created_at, updated_at
            from user_profile
            where user_id = #{userId}
            limit 1
            """)
    UserProfileEntity findByUserId(Long userId);

    @Update("""
            update user_profile
            set nickname = #{nickname},
                avatar_url = #{avatarUrl},
                bio = #{bio},
                updated_at = current_timestamp(3)
            where user_id = #{userId}
            """)
    int update(UserProfileEntity entity);
}

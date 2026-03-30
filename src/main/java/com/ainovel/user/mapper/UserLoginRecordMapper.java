package com.ainovel.user.mapper;

import com.ainovel.user.entity.UserLoginRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserLoginRecordMapper {

    @Insert("""
            insert into user_login_record (
                user_id, username_attempt, success, ip_address, device_id, lock_triggered
            ) values (
                #{userId}, #{usernameAttempt}, #{success}, #{ipAddress}, #{deviceId}, #{lockTriggered}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserLoginRecordEntity entity);

    @Select("""
            select id, user_id, username_attempt, success, ip_address, device_id, lock_triggered, created_at
            from user_login_record
            where user_id = #{userId}
            order by created_at desc
            limit #{size} offset #{offset}
            """)
    List<UserLoginRecordEntity> queryByUser(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    @Select("""
            select count(1)
            from user_login_record
            where user_id = #{userId}
            """)
    long countByUser(@Param("userId") Long userId);
}

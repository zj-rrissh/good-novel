package com.ainovel.user.mapper;

import com.ainovel.user.entity.UserAccountEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserAccountMapper {

    @Insert("""
            insert into user_account (username, password_hash, status, roles, login_version)
            values (#{username}, #{passwordHash}, #{status}, #{roles}, #{loginVersion})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccountEntity entity);

    @Select("""
            select id, username, password_hash, status, roles, login_version, failed_login_count, locked_until, created_at, updated_at
            from user_account
            where username = #{username}
            limit 1
            """)
    UserAccountEntity findByUsername(String username);

    @Select("""
            select id, username, password_hash, status, roles, login_version, failed_login_count, locked_until, created_at, updated_at
            from user_account
            where id = #{userId}
            limit 1
            """)
    UserAccountEntity findById(Long userId);

    @Select("""
            select login_version
            from user_account
            where id = #{userId}
            """)
    Long findLoginVersion(Long userId);

    @Update("""
            update user_account
            set login_version = login_version + 1,
                updated_at = current_timestamp(3)
            where id = #{userId}
            """)
    int incrementLoginVersion(Long userId);

    @Update("""
            update user_account
            set password_hash = #{passwordHash},
                updated_at = current_timestamp(3)
            where id = #{userId}
            """)
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("""
            update user_account
            set failed_login_count = failed_login_count + 1,
                updated_at = current_timestamp(3)
            where id = #{userId}
            """)
    int incrementFailedLoginCount(Long userId);

    @Update("""
            update user_account
            set failed_login_count = 0,
                locked_until = null,
                updated_at = current_timestamp(3)
            where id = #{userId}
            """)
    int resetFailedLoginCount(Long userId);

    @Update("""
            update user_account
            set failed_login_count = #{count},
                locked_until = #{lockedUntil},
                updated_at = current_timestamp(3)
            where id = #{userId}
            """)
    int lockAccount(@Param("userId") Long userId,
                    @Param("count") int count,
                    @Param("lockedUntil") LocalDateTime lockedUntil);
}


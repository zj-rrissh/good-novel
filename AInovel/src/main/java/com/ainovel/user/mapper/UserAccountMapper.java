package com.ainovel.user.mapper;

import com.ainovel.user.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
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
            select id, username, password_hash, status, roles, login_version, created_at, updated_at
            from user_account
            where username = #{username}
            limit 1
            """)
    UserAccountEntity findByUsername(String username);

    @Select("""
            select id, username, password_hash, status, roles, login_version, created_at, updated_at
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
}

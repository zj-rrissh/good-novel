package com.ainovel.reading.mapper;

import com.ainovel.reading.entity.ReadingHistoryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ReadingHistoryMapper {

    @Select("""
            select id, user_id, novel_id, chapter_id, last_read_at
            from reading_history
            where user_id = #{userId}
              and novel_id = #{novelId}
            limit 1
            """)
    ReadingHistoryEntity findByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Insert("""
            insert into reading_history (user_id, novel_id, chapter_id, last_read_at)
            values (#{userId}, #{novelId}, #{chapterId}, current_timestamp(3))
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReadingHistoryEntity entity);

    @Update("""
            update reading_history
            set chapter_id = #{chapterId},
                last_read_at = current_timestamp(3)
            where user_id = #{userId}
              and novel_id = #{novelId}
            """)
    int update(ReadingHistoryEntity entity);

    @Select("""
            select id, user_id, novel_id, chapter_id, last_read_at
            from reading_history
            where user_id = #{userId}
            order by last_read_at desc
            limit #{limit} offset #{offset}
            """)
    List<ReadingHistoryEntity> findRecentByUser(@Param("userId") Long userId,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    @Select("""
            select count(*)
            from reading_history
            where user_id = #{userId}
            """)
    long countByUser(@Param("userId") Long userId);

    @Delete("""
            delete from reading_history
            where user_id = #{userId}
              and novel_id = #{novelId}
            """)
    int deleteByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);
}

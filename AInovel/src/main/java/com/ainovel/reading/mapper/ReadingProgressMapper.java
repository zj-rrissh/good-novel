package com.ainovel.reading.mapper;

import com.ainovel.reading.entity.ReadingProgressEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ReadingProgressMapper {

    @Select("""
            select user_id, novel_id, chapter_id, progress_percent, page_offset, created_at, updated_at
            from reading_progress
            where user_id = #{userId}
              and novel_id = #{novelId}
            limit 1
            """)
    ReadingProgressEntity findByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Insert("""
            insert into reading_progress (user_id, novel_id, chapter_id, progress_percent, page_offset)
            values (#{userId}, #{novelId}, #{chapterId}, #{progressPercent}, #{pageOffset})
            """)
    int insert(ReadingProgressEntity entity);

    @Update("""
            update reading_progress
            set chapter_id = #{chapterId},
                progress_percent = #{progressPercent},
                page_offset = #{pageOffset},
                updated_at = current_timestamp(3)
            where user_id = #{userId}
              and novel_id = #{novelId}
            """)
    int update(ReadingProgressEntity entity);
}

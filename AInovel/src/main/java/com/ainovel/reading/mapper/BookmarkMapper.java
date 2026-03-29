package com.ainovel.reading.mapper;

import com.ainovel.reading.entity.BookmarkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BookmarkMapper {

    @Select("""
            select id, user_id, novel_id, chapter_id, page_offset, note, created_at
            from bookmark
            where user_id = #{userId}
              and chapter_id = #{chapterId}
            limit 1
            """)
    BookmarkEntity findByUserAndChapter(@Param("userId") Long userId, @Param("chapterId") Long chapterId);

    @Select("""
            select id, user_id, novel_id, chapter_id, page_offset, note, created_at
            from bookmark
            where user_id = #{userId}
              and novel_id = #{novelId}
            order by created_at desc
            limit #{limit} offset #{offset}
            """)
    List<BookmarkEntity> findByUserAndNovel(@Param("userId") Long userId,
                                            @Param("novelId") Long novelId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    @Select("""
            select count(*)
            from bookmark
            where user_id = #{userId}
              and novel_id = #{novelId}
            """)
    long countByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Select("""
            select id, user_id, novel_id, chapter_id, page_offset, note, created_at
            from bookmark
            where user_id = #{userId}
            order by created_at desc
            limit #{limit} offset #{offset}
            """)
    List<BookmarkEntity> findAllByUser(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("""
            select count(*)
            from bookmark
            where user_id = #{userId}
            """)
    long countAllByUser(@Param("userId") Long userId);

    @Insert("""
            insert into bookmark (user_id, novel_id, chapter_id, page_offset, note)
            values (#{userId}, #{novelId}, #{chapterId}, #{pageOffset}, #{note})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BookmarkEntity entity);

    @Delete("""
            delete from bookmark
            where id = #{id}
              and user_id = #{userId}
            """)
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}

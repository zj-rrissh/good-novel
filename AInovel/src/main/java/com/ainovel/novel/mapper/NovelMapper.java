package com.ainovel.novel.mapper;

import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.NovelEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NovelMapper {

    @Insert("""
            insert into novel (
                author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id
            ) values (
                #{authorId}, #{title}, #{intro}, #{coverUrl}, #{categoryId}, #{tagIds}, #{status}, #{latestChapterId},
                #{wordCount}, #{auditTaskId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NovelEntity entity);

    @Select("""
            select id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count,
                   audit_task_id, created_at, updated_at
            from novel
            where id = #{novelId}
            limit 1
            """)
    NovelEntity findById(Long novelId);

    @Select("""
            select id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count,
                   audit_task_id, created_at, updated_at
            from novel
            where audit_task_id = #{auditTaskId}
            limit 1
            """)
    NovelEntity findByAuditTaskId(String auditTaskId);

    @Update("""
            update novel
            set title = #{title},
                intro = #{intro},
                cover_url = #{coverUrl},
                category_id = #{categoryId},
                tag_ids = #{tagIds},
                updated_at = current_timestamp(3)
            where id = #{id}
              and author_id = #{authorId}
            """)
    int updateDraft(NovelEntity entity);

    @Update("""
            <script>
            update novel
            set status = #{targetStatus},
                updated_at = current_timestamp(3)
            where id = #{novelId}
            <if test='allowedStatuses != null and allowedStatuses.size() > 0'>
                and status in
                <foreach collection='allowedStatuses' item='status' open='(' separator=',' close=')'>
                    #{status}
                </foreach>
            </if>
            </script>
            """)
    int updateStatus(@Param("novelId") Long novelId,
                     @Param("targetStatus") NovelStatus targetStatus,
                     @Param("allowedStatuses") List<NovelStatus> allowedStatuses);

    @Update("""
            <script>
            update novel
            set status = #{targetStatus},
                audit_task_id = #{auditTaskId},
                updated_at = current_timestamp(3)
            where id = #{novelId}
            <if test='allowedStatuses != null and allowedStatuses.size() > 0'>
                and status in
                <foreach collection='allowedStatuses' item='status' open='(' separator=',' close=')'>
                    #{status}
                </foreach>
            </if>
            </script>
            """)
    int markPendingAudit(@Param("novelId") Long novelId,
                         @Param("auditTaskId") String auditTaskId,
                         @Param("targetStatus") NovelStatus targetStatus,
                         @Param("allowedStatuses") List<NovelStatus> allowedStatuses);

    @Update("""
            update novel
            set status = #{targetStatus},
                updated_at = current_timestamp(3)
            where audit_task_id = #{auditTaskId}
            """)
    int applyAuditResultByTaskId(@Param("auditTaskId") String auditTaskId,
                                 @Param("targetStatus") NovelStatus targetStatus);

    @Update("""
            update novel
            set latest_chapter_id = #{latestChapterId},
                word_count = #{wordCount},
                updated_at = current_timestamp(3)
            where id = #{novelId}
            """)
    int updateStatistics(@Param("novelId") Long novelId,
                         @Param("latestChapterId") Long latestChapterId,
                         @Param("wordCount") Long wordCount);
}

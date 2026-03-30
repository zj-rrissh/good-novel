package com.ainovel.novel.mapper;

import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.entity.ChapterEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ChapterMapper {

    @Insert("""
            insert into novel_chapter (
                novel_id, chapter_no, title, content, status, audit_task_id, published_at
            ) values (
                #{novelId}, #{chapterNo}, #{title}, #{content}, #{status}, #{auditTaskId}, #{publishedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChapterEntity entity);

    @Select("""
            select id, novel_id, chapter_no, title, content, status, audit_task_id, published_at, created_at, updated_at
            from novel_chapter
            where id = #{chapterId}
            limit 1
            """)
    ChapterEntity findById(Long chapterId);

    @Select("""
            select id, novel_id, chapter_no, title, content, status, audit_task_id, published_at, created_at, updated_at
            from novel_chapter
            where id = #{chapterId}
              and status = 'PUBLISHED'
            limit 1
            """)
    ChapterEntity findPublishedById(Long chapterId);

    @Select("""
            select id, novel_id, chapter_no, title, content, status, audit_task_id, published_at, created_at, updated_at
            from novel_chapter
            where novel_id = #{novelId}
            order by chapter_no asc, id asc
            """)
    List<ChapterEntity> findByNovelId(Long novelId);

    @Select("""
            select id, novel_id, chapter_no, title, content, status, audit_task_id, published_at, created_at, updated_at
            from novel_chapter
            where novel_id = #{novelId}
              and status = 'PUBLISHED'
            order by chapter_no asc, id asc
            """)
    List<ChapterEntity> findPublishedByNovelId(Long novelId);

    @Select("""
            <script>
            select id, novel_id, chapter_no, title, content, status, audit_task_id, published_at, created_at, updated_at
            from novel_chapter
            where novel_id = #{novelId}
              and id in
              <foreach collection='chapterIds' item='chapterId' open='(' separator=',' close=')'>
                  #{chapterId}
              </foreach>
            order by chapter_no asc, id asc
            </script>
            """)
    List<ChapterEntity> findByNovelIdAndIds(@Param("novelId") Long novelId, @Param("chapterIds") Set<Long> chapterIds);

    @Select("""
            select id
            from novel_chapter
            where novel_id = #{novelId}
            """)
    List<Long> findIdsByNovelId(Long novelId);

    @Update("""
            update novel_chapter
            set chapter_no = #{chapterNo},
                title = #{title},
                content = #{content},
                updated_at = current_timestamp(3)
            where id = #{id}
              and novel_id = #{novelId}
            """)
    int updateDraft(ChapterEntity entity);

    @Update("""
            update novel_chapter
            set chapter_no = #{chapterNo},
                title = #{title},
                content = #{content},
                status = #{status},
                audit_task_id = #{auditTaskId},
                published_at = #{publishedAt},
                updated_at = current_timestamp(3)
            where id = #{id}
              and novel_id = #{novelId}
            """)
    int updateImported(ChapterEntity entity);

    @Update("""
            <script>
            update novel_chapter
            set status = #{targetStatus},
                audit_task_id = #{auditTaskId},
                updated_at = current_timestamp(3)
            where novel_id = #{novelId}
            <if test='chapterIds != null and chapterIds.size() > 0'>
                and id in
                <foreach collection='chapterIds' item='chapterId' open='(' separator=',' close=')'>
                    #{chapterId}
                </foreach>
            </if>
            <if test='allowedStatuses != null and allowedStatuses.size() > 0'>
                and status in
                <foreach collection='allowedStatuses' item='status' open='(' separator=',' close=')'>
                    #{status}
                </foreach>
            </if>
            </script>
            """)
    int markPendingAudit(@Param("novelId") Long novelId,
                         @Param("chapterIds") Set<Long> chapterIds,
                         @Param("auditTaskId") String auditTaskId,
                         @Param("targetStatus") ChapterStatus targetStatus,
                         @Param("allowedStatuses") List<ChapterStatus> allowedStatuses);

    @Update("""
            update novel_chapter
            set status = #{targetStatus},
                published_at = #{publishedAt},
                updated_at = current_timestamp(3)
            where audit_task_id = #{auditTaskId}
            """)
    int applyAuditResultByTaskId(@Param("auditTaskId") String auditTaskId,
                                 @Param("targetStatus") ChapterStatus targetStatus,
                                 @Param("publishedAt") LocalDateTime publishedAt);

    @Select("""
            select coalesce(sum(length(content)), 0)
            from novel_chapter
            where novel_id = #{novelId}
              and status = 'PUBLISHED'
            """)
    Long sumPublishedWordCount(Long novelId);

    @Select("""
            select id
            from novel_chapter
            where novel_id = #{novelId}
              and status = 'PUBLISHED'
            order by chapter_no desc, id desc
            limit 1
            """)
    Long findLatestPublishedChapterId(Long novelId);
}

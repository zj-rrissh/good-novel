package com.ainovel.community.mapper;

import com.ainovel.community.entity.CommunityPartitionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPartitionMapper {

    @Select("""
            select
                id,
                novel_id,
                partition_key,
                partition_name,
                sort_order,
                status
            from novel_community_partition
            where novel_id = #{novelId}
              and status = 'ACTIVE'
            order by sort_order asc, id asc
            """)
    List<CommunityPartitionEntity> queryActiveByNovelId(@Param("novelId") Long novelId);

    @Select("""
            select exists(
                select 1
                from novel_community_partition
                where id = #{partitionId}
                  and status = 'ACTIVE'
            )
            """)
    boolean existsActiveById(@Param("partitionId") Long partitionId);
}

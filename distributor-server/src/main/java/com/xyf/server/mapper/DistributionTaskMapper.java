package com.xyf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xyf.server.domain.DistributionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 分发任务 Mapper
 */
@Mapper
public interface DistributionTaskMapper extends BaseMapper<DistributionTask> {

    /**
     * 乐观锁拾取任务：仅当状态为 PENDING 时更新为 UPLOADING
     *
     * @param id 任务ID
     * @return 影响行数（1=拾取成功，0=已被其他线程拾取）
     */
    @Update("UPDATE distribution_task SET status = 'UPLOADING', started_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING' AND is_deleted = 0")
    int claimTask(@Param("id") Long id);
}

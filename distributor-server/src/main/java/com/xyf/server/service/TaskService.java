package com.xyf.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xyf.server.common.BusinessException;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.DistributionTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 分发任务服务
 */
@Service
public class TaskService {

    private final DistributionTaskMapper taskMapper;

    public TaskService(DistributionTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 创建分发任务（含幂等性检查）
     */
    public DistributionTask createTask(Long videoId, String platform, Long accountId) {
        // 幂等性检查：同一 (video_id, platform, account_id) 不重复创建
        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<DistributionTask>()
                .eq(DistributionTask::getVideoId, videoId)
                .eq(DistributionTask::getPlatform, platform)
                .eq(DistributionTask::getAccountId, accountId)
                .in(DistributionTask::getStatus, "PENDING", "UPLOADING", "PROCESSING", "SUCCESS");

        DistributionTask existing = taskMapper.selectOne(wrapper);
        if (existing != null) {
            if ("SUCCESS".equals(existing.getStatus())) {
                return existing; // 已成功，直接返回
            }
            return existing; // 进行中，返回已有任务
        }

        // 创建新任务
        DistributionTask task = new DistributionTask();
        task.setUserId(1L);
        task.setVideoId(videoId);
        task.setPlatform(platform.toUpperCase());
        task.setAccountId(accountId);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setScheduledAt(LocalDateTime.now());

        // ext_info 写入 traceId
        Map<String, Object> extInfo = new HashMap<>();
        extInfo.put("traceId", TraceContext.getTraceId());
        task.setExtInfo(extInfo);

        taskMapper.insert(task);
        return task;
    }

    /**
     * 获取任务详情
     */
    public DistributionTask getTask(Long id) {
        DistributionTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "Task not found: " + id);
        }
        return task;
    }

    /**
     * 分页列出任务
     */
    public IPage<DistributionTask> listTasks(int page, int size, Long videoId, String platform) {
        size = Math.min(size, 100);
        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<>();
        if (videoId != null) {
            wrapper.eq(DistributionTask::getVideoId, videoId);
        }
        if (platform != null && !platform.isBlank()) {
            wrapper.eq(DistributionTask::getPlatform, platform.toUpperCase());
        }
        wrapper.orderByDesc(DistributionTask::getCreatedAt);

        Page<DistributionTask> pageParam = new Page<>(page, size);
        return taskMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 重试任务
     */
    public DistributionTask retryTask(Long id) {
        DistributionTask task = getTask(id);
        if (!"FAILED".equals(task.getStatus())) {
            throw new BusinessException("INVALID_STATUS", "Only FAILED tasks can be retried, current: " + task.getStatus());
        }

        task.setStatus("PENDING");
        task.setScheduledAt(LocalDateTime.now());
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        return task;
    }

    /**
     * 取消任务
     */
    public DistributionTask cancelTask(Long id) {
        DistributionTask task = getTask(id);
        if ("SUCCESS".equals(task.getStatus())) {
            throw new BusinessException("INVALID_STATUS", "Cannot cancel a completed task");
        }
        if ("UPLOADING".equals(task.getStatus()) || "PROCESSING".equals(task.getStatus())) {
            throw new BusinessException("INVALID_STATUS", "Cannot cancel a task in progress");
        }

        task.setStatus("FAILED");
        task.setErrorMessage("Cancelled by user");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }
}

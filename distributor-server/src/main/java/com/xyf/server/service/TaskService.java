package com.xyf.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.enums.PlatformType;
import com.xyf.server.domain.enums.TaskStatus;
import com.xyf.server.log.TraceContext;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.mapper.DistributionTaskMapper;
import com.xyf.server.mapper.PlatformAccountMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaskService {

    private final DistributionTaskMapper taskMapper;
    private final PlatformAccountMapper accountMapper;

    public TaskService(DistributionTaskMapper taskMapper, PlatformAccountMapper accountMapper) {
        this.taskMapper = taskMapper;
        this.accountMapper = accountMapper;
    }

    public DistributionTask createTask(Long videoId, PlatformType platform, Long accountId) {
        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<DistributionTask>()
                .eq(DistributionTask::getVideoId, videoId)
                .eq(DistributionTask::getPlatform, platform)
                .eq(DistributionTask::getAccountId, accountId)
                .in(DistributionTask::getStatus,
                        TaskStatus.PENDING, TaskStatus.UPLOADING, TaskStatus.PROCESSING, TaskStatus.SUCCESS);

        DistributionTask existing = taskMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        DistributionTask task = new DistributionTask();
        task.setUserId(BizConstants.DEFAULT_USER_ID);
        task.setVideoId(videoId);
        task.setPlatform(platform);
        task.setAccountId(accountId);
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setMaxRetry(BizConstants.DEFAULT_MAX_RETRY);
        task.setScheduledAt(LocalDateTime.now());

        Map<String, Object> extInfo = new HashMap<>(4);
        extInfo.put("traceId", TraceContext.getTraceId());
        task.setExtInfo(extInfo);

        taskMapper.insert(task);
        return task;
    }

    public DistributionTask getTask(Long id) {
        DistributionTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + id);
        }
        return task;
    }

    public IPage<DistributionTask> listTasks(int page, int size, Long videoId, PlatformType platform) {
        size = Math.min(size, BizConstants.PAGE_SIZE_MAX);
        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<>();
        if (videoId != null) {
            wrapper.eq(DistributionTask::getVideoId, videoId);
        }
        if (platform != null) {
            wrapper.eq(DistributionTask::getPlatform, platform);
        }
        wrapper.orderByDesc(DistributionTask::getCreatedAt);

        Page<DistributionTask> pageParam = new Page<>(page, size);
        return taskMapper.selectPage(pageParam, wrapper);
    }

    public DistributionTask retryTask(Long id) {
        DistributionTask task = getTask(id);
        if (task.getStatus() != TaskStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS,
                    "Only FAILED tasks can be retried, current: " + task.getStatus());
        }

        task.setStatus(TaskStatus.PENDING);
        task.setScheduledAt(LocalDateTime.now());
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        return task;
    }

    public DistributionTask cancelTask(Long id) {
        DistributionTask task = getTask(id);
        if (task.getStatus() == TaskStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "Cannot cancel a completed task");
        }
        if (task.getStatus() == TaskStatus.UPLOADING || task.getStatus() == TaskStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "Cannot cancel a task in progress");
        }

        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage("Cancelled by user");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }

    public PlatformAccount getAccount(Long accountId) {
        return accountMapper.selectById(accountId);
    }

    public void updateAccount(PlatformAccount account) {
        accountMapper.updateById(account);
    }
}

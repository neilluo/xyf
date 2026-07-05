package com.xyf.server.scheduler;

import com.xyf.server.domain.DistributionTask;
import com.xyf.server.mapper.DistributionTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 任务重试策略 - 指数退避
 * <p>
 * delay = min(30s × 2^(retryCount-1), 10m)
 * 即: 30s, 1m, 2m, 4m, 8m, 10m(cap)
 */
@Component
public class RetryStrategy {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategy.class);
    private static final long BASE_DELAY_SECONDS = 30;
    private static final long MAX_DELAY_SECONDS = 600; // 10 minutes

    private final DistributionTaskMapper taskMapper;

    public RetryStrategy(DistributionTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 处理任务失败 - 决定重试或永久失败
     */
    public void handleFailure(DistributionTask task, String errorMessage) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(errorMessage);

        if (task.getRetryCount() >= task.getMaxRetry()) {
            // 超过最大重试次数，永久标记 FAILED
            task.setStatus("FAILED");
            task.setCompletedAt(LocalDateTime.now());
            log.warn("Task permanently failed after {} retries: id={}", task.getRetryCount(), task.getId());
        } else {
            // 计算下次重试时间（指数退避）
            long delaySeconds = calculateDelay(task.getRetryCount());
            task.setStatus("PENDING");
            task.setScheduledAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("Task scheduled for retry: id={}, retryCount={}, nextRetryIn={}s",
                    task.getId(), task.getRetryCount(), delaySeconds);
        }

        taskMapper.updateById(task);
    }

    /**
     * 计算指数退避延迟
     */
    long calculateDelay(int retryCount) {
        long delay = BASE_DELAY_SECONDS * (long) Math.pow(2, retryCount - 1);
        return Math.min(delay, MAX_DELAY_SECONDS);
    }
}

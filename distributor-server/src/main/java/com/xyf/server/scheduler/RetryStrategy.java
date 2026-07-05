package com.xyf.server.scheduler;

import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.enums.TaskStatus;
import com.xyf.server.mapper.DistributionTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RetryStrategy {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategy.class);
    private static final long BASE_DELAY_SECONDS = 30;
    private static final long MAX_DELAY_SECONDS = 600;

    private final DistributionTaskMapper taskMapper;

    public RetryStrategy(DistributionTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public void handleFailure(DistributionTask task, String errorMessage) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(errorMessage);

        if (task.getRetryCount() >= task.getMaxRetry()) {
            task.setStatus(TaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            log.warn("Task permanently failed after {} retries: id={}", task.getRetryCount(), task.getId());
        } else {
            long delaySeconds = calculateDelay(task.getRetryCount());
            task.setStatus(TaskStatus.PENDING);
            task.setScheduledAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("Task scheduled for retry: id={}, retryCount={}, nextRetryIn={}s",
                    task.getId(), task.getRetryCount(), delaySeconds);
        }

        taskMapper.updateById(task);
    }

    long calculateDelay(int retryCount) {
        long delay = BASE_DELAY_SECONDS * (long) Math.pow(2, retryCount - 1);
        return Math.min(delay, MAX_DELAY_SECONDS);
    }
}

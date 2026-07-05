package com.xyf.server.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.mapper.DistributionTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 任务调度器 - DB轮询 + 异步执行
 * <p>
 * fixedDelay=10000: 上一次执行完成后等10秒再扫描
 * 使用乐观锁防重复拾取
 */
@Component
public class DistributionTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(DistributionTaskScheduler.class);
    private static final int BATCH_SIZE = 10;

    private final DistributionTaskMapper taskMapper;
    private final DistributionOrchestrator orchestrator;
    private final Executor taskExecutor;

    private volatile boolean shutdownRequested = false;

    public DistributionTaskScheduler(DistributionTaskMapper taskMapper,
                         DistributionOrchestrator orchestrator,
                         @Qualifier("taskExecutor") Executor taskExecutor) {
        this.taskMapper = taskMapper;
        this.orchestrator = orchestrator;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 定时轮询 PENDING 任务
     */
    @Scheduled(fixedDelay = 10000)
    public void pollAndDispatch() {
        if (shutdownRequested) {
            log.info("Shutdown requested, skipping task polling");
            return;
        }

        // 查询到达调度时间的 PENDING 任务
        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<DistributionTask>()
                .eq(DistributionTask::getStatus, "PENDING")
                .le(DistributionTask::getScheduledAt, LocalDateTime.now())
                .orderByAsc(DistributionTask::getScheduledAt)
                .last("LIMIT " + BATCH_SIZE);

        List<DistributionTask> tasks = taskMapper.selectList(wrapper);

        for (DistributionTask task : tasks) {
            if (shutdownRequested) break;

            // 乐观锁拾取
            int claimed = taskMapper.claimTask(task.getId());
            if (claimed == 1) {
                log.info("Claimed task id={}, submitting to executor", task.getId());
                task.setStatus("UPLOADING"); // 本地更新状态，避免再次查询
                taskExecutor.execute(() -> orchestrator.execute(task));
            }
        }
    }

    /**
     * 请求停机
     */
    public void requestShutdown() {
        this.shutdownRequested = true;
        log.info("TaskScheduler shutdown requested");
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}

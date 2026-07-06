package com.xyf.server.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.enums.TaskStatus;
import com.xyf.server.mapper.DistributionTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

@Component
public class DistributionTaskScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DistributionTaskScheduler.class);

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
     * 启动时回滚孤儿任务：将 UPLOADING/PROCESSING 状态的任务重置为 PENDING。
     * 这些任务是上次服务中断时正在执行的，需要重新调度。
     */
    @Override
    public void run(ApplicationArguments args) {
        LambdaUpdateWrapper<DistributionTask> wrapper = new LambdaUpdateWrapper<DistributionTask>()
                .in(DistributionTask::getStatus, TaskStatus.UPLOADING, TaskStatus.PROCESSING)
                .set(DistributionTask::getStatus, TaskStatus.PENDING)
                .set(DistributionTask::getScheduledAt, LocalDateTime.now());

        int recovered = taskMapper.update(null, wrapper);
        if (recovered > 0) {
            log.info("Recovered {} orphaned tasks (UPLOADING/PROCESSING → PENDING) on startup", recovered);
        }
    }

    @Scheduled(fixedDelay = BizConstants.SCHEDULER_POLL_INTERVAL_MS)
    public void pollAndDispatch() {
        if (shutdownRequested) {
            log.info("Shutdown requested, skipping task polling");
            return;
        }

        LambdaQueryWrapper<DistributionTask> wrapper = new LambdaQueryWrapper<DistributionTask>()
                .eq(DistributionTask::getStatus, TaskStatus.PENDING)
                .le(DistributionTask::getScheduledAt, LocalDateTime.now())
                .orderByAsc(DistributionTask::getScheduledAt);

        Page<DistributionTask> page = new Page<>(1, BizConstants.SCHEDULER_BATCH_SIZE, false);
        List<DistributionTask> tasks = taskMapper.selectPage(page, wrapper).getRecords();

        for (DistributionTask task : tasks) {
            if (shutdownRequested) {
                break;
            }

            int claimed = taskMapper.claimTask(task.getId());
            if (claimed == 1) {
                log.info("Claimed task id={}, submitting to executor", task.getId());
                task.setStatus(TaskStatus.UPLOADING);
                taskExecutor.execute(() -> orchestrator.execute(task));
            }
        }
    }

    public void requestShutdown() {
        this.shutdownRequested = true;
        log.info("TaskScheduler shutdown requested");
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}

package com.xyf.server.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * 优雅停机管理器
 * <p>
 * 收到 SIGTERM 后：
 * 1. 标记 shutdownFlag → 不再拾取新任务
 * 2. 等待当前任务完成（最长5分钟）
 * 3. 超时强制退出，依赖断点续传恢复
 */
@Component
public class GracefulShutdownManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownManager.class);

    private final DistributionTaskScheduler scheduler;

    public GracefulShutdownManager(DistributionTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void destroy() {
        log.info("Graceful shutdown initiated...");
        scheduler.requestShutdown();
        log.info("TaskScheduler stopped accepting new tasks. Waiting for in-flight tasks to complete...");
        // Executor的awaitTermination由TtlExecutorConfig配置（300秒）
        // Spring容器会等待Executor中的任务完成
        log.info("Graceful shutdown complete.");
    }
}

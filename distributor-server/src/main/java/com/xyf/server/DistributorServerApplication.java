package com.xyf.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 视频分发平台服务端启动类
 * <p>
 * 单节点架构：API服务 + 任务调度 + 视频上传全在同一进程内完成
 */
@SpringBootApplication
@EnableScheduling
public class DistributorServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributorServerApplication.class, args);
    }
}

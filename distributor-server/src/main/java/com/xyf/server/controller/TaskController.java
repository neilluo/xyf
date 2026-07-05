package com.xyf.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xyf.server.common.ApiResponse;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 分发任务 REST API
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 创建分发任务 */
    @PostMapping
    public ApiResponse<DistributionTask> createTask(@RequestBody Map<String, Object> body) {
        Long videoId = Long.valueOf(body.get("videoId").toString());
        String platform = body.get("platform").toString();
        Long accountId = Long.valueOf(body.get("accountId").toString());
        return ApiResponse.ok(taskService.createTask(videoId, platform, accountId));
    }

    /** 列出分发任务 */
    @GetMapping
    public ApiResponse<IPage<DistributionTask>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long videoId,
            @RequestParam(required = false) String platform) {
        return ApiResponse.ok(taskService.listTasks(page, size, videoId, platform));
    }

    /** 获取任务详情 */
    @GetMapping("/{id}")
    public ApiResponse<DistributionTask> getTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.getTask(id));
    }

    /** 重试任务 */
    @PostMapping("/{id}/retry")
    public ApiResponse<DistributionTask> retryTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.retryTask(id));
    }

    /** 取消任务 */
    @PostMapping("/{id}/cancel")
    public ApiResponse<DistributionTask> cancelTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.cancelTask(id));
    }
}

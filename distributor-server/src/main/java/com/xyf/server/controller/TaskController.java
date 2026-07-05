package com.xyf.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xyf.server.common.ApiResponse;
import com.xyf.server.controller.dto.CreateTaskRequest;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.enums.PlatformType;
import com.xyf.server.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<DistributionTask> createTask(@Valid @RequestBody CreateTaskRequest request) {
        PlatformType platform = PlatformType.fromCode(request.getPlatform());
        return ApiResponse.ok(taskService.createTask(request.getVideoId(), platform, request.getAccountId()));
    }

    @GetMapping
    public ApiResponse<IPage<DistributionTask>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long videoId,
            @RequestParam(required = false) String platform) {
        PlatformType platformType = (platform != null && !platform.isBlank()) ? PlatformType.fromCode(platform) : null;
        return ApiResponse.ok(taskService.listTasks(page, size, videoId, platformType));
    }

    @GetMapping("/{id}")
    public ApiResponse<DistributionTask> getTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.getTask(id));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<DistributionTask> retryTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.retryTask(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<DistributionTask> cancelTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.cancelTask(id));
    }
}

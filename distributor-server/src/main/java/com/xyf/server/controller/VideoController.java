package com.xyf.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xyf.server.common.ApiResponse;
import com.xyf.server.controller.dto.CreateVideoRequest;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.service.VideoService;
import com.xyf.server.storage.StsCredentials;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ApiResponse<VideoMeta> createVideo(@Valid @RequestBody CreateVideoRequest request) {
        return ApiResponse.ok(videoService.createVideo(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoMeta> getVideo(@PathVariable Long id) {
        return ApiResponse.ok(videoService.getVideo(id));
    }

    @GetMapping
    public ApiResponse<IPage<VideoMeta>> listVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(videoService.listVideos(page, size));
    }

    @PostMapping("/upload-token")
    public ApiResponse<StsCredentials> getUploadToken() {
        return ApiResponse.ok(videoService.getUploadToken());
    }
}

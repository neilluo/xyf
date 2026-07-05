package com.xyf.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xyf.server.common.ApiResponse;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.service.VideoService;
import com.xyf.server.storage.StsCredentials;
import org.springframework.web.bind.annotation.*;

/**
 * 视频管理 REST API
 */
@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /** 注册视频元数据 */
    @PostMapping
    public ApiResponse<VideoMeta> createVideo(@RequestBody VideoMeta video) {
        return ApiResponse.ok(videoService.createVideo(video));
    }

    /** 获取视频详情 */
    @GetMapping("/{id}")
    public ApiResponse<VideoMeta> getVideo(@PathVariable Long id) {
        return ApiResponse.ok(videoService.getVideo(id));
    }

    /** 列出所有视频 */
    @GetMapping
    public ApiResponse<IPage<VideoMeta>> listVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(videoService.listVideos(page, size));
    }

    /** 获取 OSS STS 临时上传凭证 */
    @PostMapping("/upload-token")
    public ApiResponse<StsCredentials> getUploadToken() {
        return ApiResponse.ok(videoService.getUploadToken());
    }
}

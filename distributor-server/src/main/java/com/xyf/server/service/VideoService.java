package com.xyf.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xyf.server.common.BusinessException;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.VideoMetaMapper;
import com.xyf.server.storage.OssStorageService;
import com.xyf.server.storage.StsCredentials;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频服务
 */
@Service
public class VideoService {

    private final VideoMetaMapper videoMetaMapper;
    private final OssStorageService ossStorageService;

    public VideoService(VideoMetaMapper videoMetaMapper, OssStorageService ossStorageService) {
        this.videoMetaMapper = videoMetaMapper;
        this.ossStorageService = ossStorageService;
    }

    /**
     * 注册视频元数据
     */
    public VideoMeta createVideo(VideoMeta video) {
        // 注入 traceId 到 ext_info
        Map<String, Object> extInfo = video.getExtInfo() != null ? new HashMap<>(video.getExtInfo()) : new HashMap<>();
        extInfo.put("traceId", TraceContext.getTraceId());
        video.setExtInfo(extInfo);

        video.setUserId(1L); // 单用户
        videoMetaMapper.insert(video);
        return video;
    }

    /**
     * 获取视频详情
     */
    public VideoMeta getVideo(Long id) {
        VideoMeta video = videoMetaMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("VIDEO_NOT_FOUND", "Video not found: " + id);
        }
        return video;
    }

    /**
     * 分页列出视频
     */
    public IPage<VideoMeta> listVideos(int page, int size) {
        size = Math.min(size, 100);
        Page<VideoMeta> pageParam = new Page<>(page, size);
        return videoMetaMapper.selectPage(pageParam, null);
    }

    /**
     * 获取 STS 上传凭证
     */
    public StsCredentials getUploadToken() {
        return ossStorageService.generateStsToken("cli-upload-" + System.currentTimeMillis());
    }
}

package com.xyf.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.controller.dto.CreateVideoRequest;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.VideoMetaMapper;
import com.xyf.server.storage.OssStorageService;
import com.xyf.server.storage.StsCredentials;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VideoService {

    private final VideoMetaMapper videoMetaMapper;
    private final OssStorageService ossStorageService;

    public VideoService(VideoMetaMapper videoMetaMapper, OssStorageService ossStorageService) {
        this.videoMetaMapper = videoMetaMapper;
        this.ossStorageService = ossStorageService;
    }

    public VideoMeta createVideo(CreateVideoRequest request) {
        VideoMeta video = new VideoMeta();
        video.setUserId(BizConstants.DEFAULT_USER_ID);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setCategory(request.getCategory());
        video.setOssBucket(request.getOssBucket());
        video.setOssKey(request.getOssKey());
        video.setOssRegion(request.getOssRegion());
        video.setFileSize(request.getFileSize());
        video.setFileFormat(request.getFileFormat());
        video.setDurationSeconds(request.getDurationSeconds());
        video.setResolution(request.getResolution());
        video.setThumbnailOssKey(request.getThumbnailOssKey());

        Map<String, Object> extInfo = new HashMap<>(4);
        extInfo.put("traceId", TraceContext.getTraceId());
        video.setExtInfo(extInfo);

        videoMetaMapper.insert(video);
        return video;
    }

    public VideoMeta getVideo(Long id) {
        VideoMeta video = videoMetaMapper.selectById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "Video not found: " + id);
        }
        return video;
    }

    public IPage<VideoMeta> listVideos(int page, int size) {
        size = Math.min(size, BizConstants.PAGE_SIZE_MAX);
        Page<VideoMeta> pageParam = new Page<>(page, size);
        return videoMetaMapper.selectPage(pageParam, null);
    }

    public StsCredentials getUploadToken() {
        return ossStorageService.generateStsToken("cli-upload-" + System.currentTimeMillis());
    }
}

package com.xyf.server.platform;

import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;

import java.io.InputStream;

/**
 * 平台上传接口 - 各平台实现此接口
 */
public interface PlatformUploader {

    /** 返回支持的平台名称 */
    String platform();

    /** 初始化上传会话 */
    UploadSession initUpload(VideoMeta video, PlatformAccount account);

    /** 流式上传（避免大文件加载到内存） */
    UploadResult uploadChunk(UploadSession session, InputStream chunkStream, long offset, long chunkSize, long totalSize);

    /** 查询上传进度 */
    long queryProgress(UploadSession session);

    /** 等待平台处理完成 */
    PublishResult waitForPublish(UploadSession session, long timeoutMs);

    /** 刷新 token */
    TokenPair refreshToken(String refreshToken);

    /** 验证 token 是否有效 */
    boolean validateToken(String accessToken);

    // --- Inner DTOs ---

    record UploadSession(String sessionUri, String platformVideoId, long totalSize) {}

    record UploadResult(boolean success, long uploadedBytes, String error, String platformVideoId) {}

    record PublishResult(boolean success, String platformVideoId, String platformUrl, String error) {}

    record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
}

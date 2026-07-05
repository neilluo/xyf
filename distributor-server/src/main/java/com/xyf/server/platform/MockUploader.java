package com.xyf.server.platform;

import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Mock 平台上传实现 - 用于验证调度流程
 */
@Component
public class MockUploader implements PlatformUploader {

    private static final Logger log = LoggerFactory.getLogger(MockUploader.class);

    @Override
    public String platform() {
        return "MOCK";
    }

    @Override
    public UploadSession initUpload(VideoMeta video, PlatformAccount account) {
        log.info("MockUploader: initUpload for video={}", video.getTitle());
        return new UploadSession("mock-session-" + System.currentTimeMillis(), null, video.getFileSize());
    }

    @Override
    public UploadResult uploadChunk(UploadSession session, InputStream chunkStream, long offset, long chunkSize, long totalSize) {
        // 模拟上传延迟
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("MockUploader: uploadChunk offset={}, chunkSize={}", offset, chunkSize);
        return new UploadResult(true, offset + chunkSize, null);
    }

    @Override
    public long queryProgress(UploadSession session) {
        return session.totalSize();
    }

    @Override
    public PublishResult waitForPublish(UploadSession session, long timeoutMs) {
        log.info("MockUploader: waitForPublish");
        return new PublishResult(true, "mock-video-id-" + System.currentTimeMillis(), "https://mock.platform/video/123", null);
    }

    @Override
    public TokenPair refreshToken(String refreshToken) {
        return new TokenPair("mock-access-token", refreshToken, 3600);
    }

    @Override
    public boolean validateToken(String accessToken) {
        return true;
    }
}

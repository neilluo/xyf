package com.xyf.server.scheduler;

import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.DistributionTaskMapper;
import com.xyf.server.mapper.PlatformAccountMapper;
import com.xyf.server.mapper.VideoMetaMapper;
import com.xyf.server.platform.PlatformUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分发编排器 - 执行完整的上传分发流程
 */
@Component
public class DistributionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DistributionOrchestrator.class);

    private final VideoMetaMapper videoMetaMapper;
    private final PlatformAccountMapper accountMapper;
    private final DistributionTaskMapper taskMapper;
    private final Map<String, PlatformUploader> uploaders;

    public DistributionOrchestrator(VideoMetaMapper videoMetaMapper,
                                    PlatformAccountMapper accountMapper,
                                    DistributionTaskMapper taskMapper,
                                    List<PlatformUploader> uploaderList) {
        this.videoMetaMapper = videoMetaMapper;
        this.accountMapper = accountMapper;
        this.taskMapper = taskMapper;
        this.uploaders = uploaderList.stream()
                .collect(Collectors.toMap(PlatformUploader::platform, Function.identity()));
    }

    /**
     * 执行分发任务
     */
    public void execute(DistributionTask task) {
        // 初始化 traceId
        String traceId = TraceContext.initTrace();
        log.info("Executing task id={}, platform={}, videoId={}", task.getId(), task.getPlatform(), task.getVideoId());

        try {
            // 1. 获取视频和账号信息
            VideoMeta video = videoMetaMapper.selectById(task.getVideoId());
            if (video == null) {
                failTask(task, "Video not found: " + task.getVideoId());
                return;
            }

            PlatformAccount account = accountMapper.selectById(task.getAccountId());
            if (account == null) {
                failTask(task, "Account not found: " + task.getAccountId());
                return;
            }

            // 2. 获取平台上传器
            PlatformUploader uploader = uploaders.get(task.getPlatform());
            if (uploader == null) {
                failTask(task, "No uploader for platform: " + task.getPlatform());
                return;
            }

            // 3. 验证/刷新 token
            if (account.getAccessToken() != null && !uploader.validateToken(account.getAccessToken())) {
                log.info("Token expired, refreshing...");
                PlatformUploader.TokenPair newToken = uploader.refreshToken(account.getRefreshToken());
                account.setAccessToken(newToken.accessToken());
                account.setRefreshToken(newToken.refreshToken());
                account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(newToken.expiresInSeconds()));
                accountMapper.updateById(account);
            }

            // 4. 初始化上传
            PlatformUploader.UploadSession session = uploader.initUpload(video, account);
            task.setUploadSessionUri(session.sessionUri());

            // 5. 模拟流式上传（实际实现会从 OSS 流式读取）
            PlatformUploader.UploadResult uploadResult = uploader.uploadChunk(
                    session, null, 0, video.getFileSize(), video.getFileSize());

            if (!uploadResult.success()) {
                failTask(task, "Upload failed: " + uploadResult.error());
                return;
            }

            // 6. 更新状态为 PROCESSING
            task.setStatus("PROCESSING");
            task.setUploadOffset(uploadResult.uploadedBytes());
            taskMapper.updateById(task);

            // 7. 等待发布完成
            PlatformUploader.PublishResult publishResult = uploader.waitForPublish(session, 300000);

            if (publishResult.success()) {
                task.setStatus("SUCCESS");
                task.setPlatformVideoId(publishResult.platformVideoId());
                task.setCompletedAt(LocalDateTime.now());
                log.info("Task completed successfully! platformVideoId={}", publishResult.platformVideoId());
            } else {
                failTask(task, "Publish failed: " + publishResult.error());
                return;
            }

            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("Task execution error", e);
            failTask(task, e.getMessage());
        } finally {
            TraceContext.clear();
        }
    }

    private void failTask(DistributionTask task, String error) {
        task.setStatus("FAILED");
        task.setErrorMessage(error);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.warn("Task failed: id={}, error={}", task.getId(), error);
    }
}

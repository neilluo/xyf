package com.xyf.server.scheduler;

import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.domain.enums.TaskStatus;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.DistributionTaskMapper;
import com.xyf.server.platform.PlatformUploader;
import com.xyf.server.service.TaskService;
import com.xyf.server.service.VideoService;
import com.xyf.server.service.auth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DistributionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DistributionOrchestrator.class);

    private final VideoService videoService;
    private final TaskService taskService;
    private final DistributionTaskMapper taskMapper;
    private final Map<String, PlatformUploader> uploaders;

    public DistributionOrchestrator(VideoService videoService,
                                    TaskService taskService,
                                    DistributionTaskMapper taskMapper,
                                    List<PlatformUploader> uploaderList) {
        this.videoService = videoService;
        this.taskService = taskService;
        this.taskMapper = taskMapper;
        this.uploaders = uploaderList.stream()
                .collect(Collectors.toMap(PlatformUploader::platform, Function.identity()));
    }

    public void execute(DistributionTask task) {
        TraceContext.initTrace();
        log.info("Executing task id={}, platform={}, videoId={}", task.getId(), task.getPlatform(), task.getVideoId());

        try {
            VideoMeta video;
            try {
                video = videoService.getVideo(task.getVideoId());
            } catch (BusinessException e) {
                failTask(task, "Video not found: " + task.getVideoId());
                return;
            }

            PlatformAccount account = taskService.getAccount(task.getAccountId());
            if (account == null) {
                failTask(task, "Account not found: " + task.getAccountId());
                return;
            }

            PlatformUploader uploader = uploaders.get(task.getPlatform().getCode());
            if (uploader == null) {
                failTask(task, "No uploader for platform: " + task.getPlatform());
                return;
            }

            if (account.getAccessToken() != null && !uploader.validateToken(account.getAccessToken())) {
                log.info("Token expired, refreshing...");
                PlatformUploader.TokenPair newToken = uploader.refreshToken(account.getRefreshToken());
                account.setAccessToken(newToken.accessToken());
                account.setRefreshToken(newToken.refreshToken());
                account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(newToken.expiresInSeconds()));
                taskService.updateAccount(account);
            }

            PlatformUploader.UploadSession session = uploader.initUpload(video, account);
            task.setUploadSessionUri(session.sessionUri());

            Long fileSize = video.getFileSize();
            if (fileSize == null || fileSize <= 0) {
                failTask(task, "Video fileSize is invalid: videoId=" + video.getId());
                return;
            }

            PlatformUploader.UploadResult uploadResult = uploader.uploadChunk(
                    session, null, 0, fileSize, fileSize);

            if (!uploadResult.success()) {
                failTask(task, "Upload failed: " + uploadResult.error());
                return;
            }

            task.setStatus(TaskStatus.PROCESSING);
            task.setUploadOffset(uploadResult.uploadedBytes());
            taskMapper.updateById(task);

            PlatformUploader.PublishResult publishResult = uploader.waitForPublish(session, BizConstants.PUBLISH_TIMEOUT_MS);

            if (publishResult.success()) {
                task.setStatus(TaskStatus.SUCCESS);
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
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(error);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.warn("Task failed: id={}, error={}", task.getId(), error);
    }
}

package com.xyf.server.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 分发任务实体
 */
@TableName(value = "distribution_task", autoResultMap = true)
public class DistributionTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long videoId;

    /** 目标平台: YOUTUBE|TIKTOK */
    private String platform;

    private Long accountId;

    /** 状态: PENDING|UPLOADING|PROCESSING|SUCCESS|FAILED */
    private String status;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String errorMessage;

    /** 平台返回的视频ID/URL */
    private String platformVideoId;

    /** 断点续传会话URI */
    private String uploadSessionUri;

    /** 已上传字节数 */
    private Long uploadOffset;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> platformMetadata;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extInfo;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getPlatformVideoId() { return platformVideoId; }
    public void setPlatformVideoId(String platformVideoId) { this.platformVideoId = platformVideoId; }

    public String getUploadSessionUri() { return uploadSessionUri; }
    public void setUploadSessionUri(String uploadSessionUri) { this.uploadSessionUri = uploadSessionUri; }

    public Long getUploadOffset() { return uploadOffset; }
    public void setUploadOffset(Long uploadOffset) { this.uploadOffset = uploadOffset; }

    public Map<String, Object> getPlatformMetadata() { return platformMetadata; }
    public void setPlatformMetadata(Map<String, Object> platformMetadata) { this.platformMetadata = platformMetadata; }

    public Map<String, Object> getExtInfo() { return extInfo; }
    public void setExtInfo(Map<String, Object> extInfo) { this.extInfo = extInfo; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

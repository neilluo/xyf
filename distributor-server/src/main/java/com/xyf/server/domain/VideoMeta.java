package com.xyf.server.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 视频元数据实体
 */
@TableName(value = "video_meta", autoResultMap = true)
public class VideoMeta {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private String category;

    private String ossBucket;

    private String ossKey;

    private String ossRegion;

    private Long fileSize;

    private String fileFormat;

    private Integer durationSeconds;

    private String resolution;

    private String thumbnailOssKey;

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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOssBucket() { return ossBucket; }
    public void setOssBucket(String ossBucket) { this.ossBucket = ossBucket; }

    public String getOssKey() { return ossKey; }
    public void setOssKey(String ossKey) { this.ossKey = ossKey; }

    public String getOssRegion() { return ossRegion; }
    public void setOssRegion(String ossRegion) { this.ossRegion = ossRegion; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getThumbnailOssKey() { return thumbnailOssKey; }
    public void setThumbnailOssKey(String thumbnailOssKey) { this.thumbnailOssKey = thumbnailOssKey; }

    public Map<String, Object> getExtInfo() { return extInfo; }
    public void setExtInfo(Map<String, Object> extInfo) { this.extInfo = extInfo; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

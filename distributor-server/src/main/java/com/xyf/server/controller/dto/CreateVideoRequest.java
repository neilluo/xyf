package com.xyf.server.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateVideoRequest {

    @NotBlank(message = "title cannot be blank")
    private String title;

    private String description;

    private List<String> tags;

    private String category;

    @NotBlank(message = "ossBucket cannot be blank")
    private String ossBucket;

    @NotBlank(message = "ossKey cannot be blank")
    private String ossKey;

    @NotBlank(message = "ossRegion cannot be blank")
    private String ossRegion;

    @NotNull(message = "fileSize cannot be null")
    private Long fileSize;

    private String fileFormat;

    private Integer durationSeconds;

    private String resolution;

    private String thumbnailOssKey;

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
}

package com.xyf.server.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTaskRequest {

    @NotNull(message = "videoId cannot be null")
    private Long videoId;

    @NotBlank(message = "platform cannot be blank")
    private String platform;

    @NotNull(message = "accountId cannot be null")
    private Long accountId;

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}

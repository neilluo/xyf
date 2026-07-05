package com.xyf.server.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xyf.server.domain.enums.AccountStatus;
import com.xyf.server.domain.enums.PlatformType;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "platform_account", autoResultMap = true)
public class PlatformAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private PlatformType platform;

    private String accountName;

    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    private String channelId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extInfo;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;

    private AccountStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public PlatformType getPlatform() { return platform; }
    public void setPlatform(PlatformType platform) { this.platform = platform; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public Map<String, Object> getExtraConfig() { return extraConfig; }
    public void setExtraConfig(Map<String, Object> extraConfig) { this.extraConfig = extraConfig; }

    public Map<String, Object> getExtInfo() { return extInfo; }
    public void setExtInfo(Map<String, Object> extInfo) { this.extInfo = extInfo; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

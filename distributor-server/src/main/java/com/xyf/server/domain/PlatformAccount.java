package com.xyf.server.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 平台账号配置实体
 */
@TableName(value = "platform_account", autoResultMap = true)
public class PlatformAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 平台: YOUTUBE|TIKTOK */
    private String platform;

    private String accountName;

    /** 访问令牌(加密存储) - 不序列化到API响应 */
    @JsonIgnore
    private String accessToken;

    /** 刷新令牌(加密存储) - 不序列化到API响应 */
    @JsonIgnore
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    /** YouTube频道ID / TikTok open_id */
    private String channelId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extInfo;

    /** 软删除：NULL=未删除，非NULL=删除时间戳 */
    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;

    /** 状态: ACTIVE|EXPIRED|REVOKED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

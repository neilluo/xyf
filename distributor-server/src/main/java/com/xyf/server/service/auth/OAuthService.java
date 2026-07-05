package com.xyf.server.service.auth;

import com.xyf.server.common.BusinessException;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.PlatformAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 授权服务 - 管理 YouTube/TikTok OAuth 流程
 */
@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final PlatformAccountMapper accountMapper;
    private final TokenEncryptService tokenEncryptService;

    @Value("${oauth.youtube.client-id:}")
    private String youtubeClientId;

    @Value("${oauth.youtube.client-secret:}")
    private String youtubeClientSecret;

    @Value("${oauth.youtube.redirect-uri:}")
    private String youtubeRedirectUri;

    @Value("${oauth.tiktok.client-key:}")
    private String tiktokClientKey;

    @Value("${oauth.tiktok.client-secret:}")
    private String tiktokClientSecret;

    @Value("${oauth.tiktok.redirect-uri:}")
    private String tiktokRedirectUri;

    /** state → 授权结果缓存（简单实现，生产建议用 Redis） */
    private final Map<String, AuthResult> authResults = new ConcurrentHashMap<>();

    public OAuthService(PlatformAccountMapper accountMapper, TokenEncryptService tokenEncryptService) {
        this.accountMapper = accountMapper;
        this.tokenEncryptService = tokenEncryptService;
    }

    /**
     * 生成授权 URL
     */
    public Map<String, String> getAuthorizeUrl(String platform, String accountName) {
        String state = UUID.randomUUID().toString().replace("-", "");

        String authUrl;
        if ("youtube".equalsIgnoreCase(platform)) {
            authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + youtubeClientId
                    + "&redirect_uri=" + youtubeRedirectUri
                    + "&response_type=code"
                    + "&scope=https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube"
                    + "&access_type=offline"
                    + "&prompt=consent"
                    + "&state=" + state;
        } else if ("tiktok".equalsIgnoreCase(platform)) {
            authUrl = "https://www.tiktok.com/v2/auth/authorize/"
                    + "?client_key=" + tiktokClientKey
                    + "&scope=video.publish,video.upload"
                    + "&response_type=code"
                    + "&redirect_uri=" + tiktokRedirectUri
                    + "&state=" + state;
        } else {
            throw new BusinessException("UNSUPPORTED_PLATFORM", "Platform not supported: " + platform);
        }

        // 记录 state 关联信息
        authResults.put(state, new AuthResult(platform, accountName, null, null, false));

        Map<String, String> result = new HashMap<>();
        result.put("authorizeUrl", authUrl);
        result.put("state", state);
        return result;
    }

    /**
     * 处理 OAuth 回调
     */
    public void handleCallback(String platform, String code, String state) {
        AuthResult pending = authResults.get(state);
        if (pending == null) {
            throw new BusinessException("INVALID_STATE", "Invalid or expired state parameter");
        }

        log.info("OAuth callback received: platform={}, state={}", platform, state);

        // TODO: 用 code 换取 token（需要实际调用 Google/TikTok token endpoint）
        // 这里先存储 code，后续 Task 12 完善真实 token 交换
        String mockAccessToken = "pending_exchange_" + code;
        String mockRefreshToken = "pending_refresh_" + code;

        // 加密存储 token
        String encryptedAccess = tokenEncryptService.encrypt(mockAccessToken);
        String encryptedRefresh = tokenEncryptService.encrypt(mockRefreshToken);

        // 保存到数据库
        PlatformAccount account = new PlatformAccount();
        account.setUserId(1L);
        account.setPlatform(platform.toUpperCase());
        account.setAccountName(pending.accountName);
        account.setAccessToken(encryptedAccess);
        account.setRefreshToken(encryptedRefresh);
        account.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
        account.setStatus("ACTIVE");

        Map<String, Object> extInfo = new HashMap<>();
        extInfo.put("traceId", TraceContext.getTraceId());
        account.setExtInfo(extInfo);

        accountMapper.insert(account);

        // 更新授权结果
        authResults.put(state, new AuthResult(platform, pending.accountName, account.getId().toString(), null, true));
        log.info("OAuth authorization completed: platform={}, accountName={}", platform, pending.accountName);
    }

    /**
     * 查询授权状态（CLI 轮询）
     */
    public Map<String, Object> getAuthStatus(String state) {
        AuthResult result = authResults.get(state);
        Map<String, Object> response = new HashMap<>();
        if (result == null) {
            response.put("status", "NOT_FOUND");
        } else if (result.completed) {
            response.put("status", "COMPLETED");
            response.put("accountId", result.accountId);
            // 清理（避免内存泄漏）
            authResults.remove(state);
        } else {
            response.put("status", "PENDING");
        }
        return response;
    }

    /**
     * 列出所有授权账号
     */
    public List<PlatformAccount> listAccounts() {
        return accountMapper.selectList(null);
    }

    private record AuthResult(String platform, String accountName, String accountId, String error, boolean completed) {}
}

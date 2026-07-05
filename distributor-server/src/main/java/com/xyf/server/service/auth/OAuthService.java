package com.xyf.server.service.auth;

import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.enums.AccountStatus;
import com.xyf.server.domain.enums.PlatformType;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.PlatformAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private final ConcurrentHashMap<String, AuthEntry> authResults = new ConcurrentHashMap<>();

    public OAuthService(PlatformAccountMapper accountMapper, TokenEncryptService tokenEncryptService) {
        this.accountMapper = accountMapper;
        this.tokenEncryptService = tokenEncryptService;
    }

    public Map<String, String> getAuthorizeUrl(String platform, String accountName) {
        PlatformType platformType = PlatformType.fromCode(platform);
        String state = UUID.randomUUID().toString().replace("-", "");

        String authUrl;
        if (platformType == PlatformType.YOUTUBE) {
            authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + youtubeClientId
                    + "&redirect_uri=" + youtubeRedirectUri
                    + "&response_type=code"
                    + "&scope=https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube"
                    + "&access_type=offline"
                    + "&prompt=consent"
                    + "&state=" + state;
        } else {
            authUrl = "https://www.tiktok.com/v2/auth/authorize/"
                    + "?client_key=" + tiktokClientKey
                    + "&scope=video.publish,video.upload"
                    + "&response_type=code"
                    + "&redirect_uri=" + tiktokRedirectUri
                    + "&state=" + state;
        }

        authResults.put(state, new AuthEntry(
                new AuthResult(platform, accountName, null, null, false),
                LocalDateTime.now()));

        Map<String, String> result = new HashMap<>(4);
        result.put("authorizeUrl", authUrl);
        result.put("state", state);
        return result;
    }

    public void handleCallback(String platform, String code, String state) {
        AuthEntry entry = authResults.get(state);
        if (entry == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Invalid or expired state parameter");
        }
        AuthResult pending = entry.result;

        log.info("OAuth callback received: platform={}, state={}", platform, state);

        String mockAccessToken = "pending_exchange_" + code;
        String mockRefreshToken = "pending_refresh_" + code;

        String encryptedAccess = tokenEncryptService.encrypt(mockAccessToken);
        String encryptedRefresh = tokenEncryptService.encrypt(mockRefreshToken);

        PlatformAccount account = new PlatformAccount();
        account.setUserId(BizConstants.DEFAULT_USER_ID);
        account.setPlatform(PlatformType.fromCode(platform));
        account.setAccountName(pending.accountName);
        account.setAccessToken(encryptedAccess);
        account.setRefreshToken(encryptedRefresh);
        account.setTokenExpiresAt(LocalDateTime.now().plusHours(BizConstants.DEFAULT_TOKEN_EXPIRE_HOURS));
        account.setStatus(AccountStatus.ACTIVE);

        Map<String, Object> extInfo = new HashMap<>(4);
        extInfo.put("traceId", TraceContext.getTraceId());
        account.setExtInfo(extInfo);

        accountMapper.insert(account);

        authResults.put(state, new AuthEntry(
                new AuthResult(platform, pending.accountName, account.getId().toString(), null, true),
                LocalDateTime.now()));
        log.info("OAuth authorization completed: platform={}, accountName={}", platform, pending.accountName);
    }

    public Map<String, Object> getAuthStatus(String state) {
        AuthEntry entry = authResults.get(state);
        Map<String, Object> response = new HashMap<>(4);
        if (entry == null) {
            response.put("status", "NOT_FOUND");
        } else if (entry.result.completed) {
            response.put("status", "COMPLETED");
            response.put("accountId", entry.result.accountId);
            authResults.remove(state);
        } else {
            response.put("status", "PENDING");
        }
        return response;
    }

    public List<PlatformAccount> listAccounts() {
        return accountMapper.selectList(null);
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredAuthEntries() {
        LocalDateTime expireThreshold = LocalDateTime.now().minusMinutes(BizConstants.AUTH_CACHE_TTL_MINUTES);
        authResults.entrySet().removeIf(e -> e.getValue().createdAt.isBefore(expireThreshold));
    }

    private record AuthResult(String platform, String accountName, String accountId, String error, boolean completed) {}

    private record AuthEntry(AuthResult result, LocalDateTime createdAt) {}
}

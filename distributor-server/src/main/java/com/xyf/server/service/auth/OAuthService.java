package com.xyf.server.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.config.DynamicConfigService;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.enums.AccountStatus;
import com.xyf.server.domain.enums.PlatformType;
import com.xyf.server.log.TraceContext;
import com.xyf.server.mapper.PlatformAccountMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final PlatformAccountMapper accountMapper;
    private final TokenEncryptService tokenEncryptService;
    private final DynamicConfigService configService;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, AuthEntry> authResults = new ConcurrentHashMap<>();

    public OAuthService(PlatformAccountMapper accountMapper,
                        TokenEncryptService tokenEncryptService,
                        DynamicConfigService configService) {
        this.accountMapper = accountMapper;
        this.tokenEncryptService = tokenEncryptService;
        this.configService = configService;
    }

    public Map<String, String> getAuthorizeUrl(String platform, String accountName) {
        PlatformType platformType = PlatformType.fromCode(platform);
        String state = UUID.randomUUID().toString().replace("-", "");

        String authUrl;
        if (platformType == PlatformType.YOUTUBE) {
            authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + configService.get("OAUTH_YOUTUBE", "client_id")
                    + "&redirect_uri=" + configService.get("OAUTH_YOUTUBE", "redirect_uri")
                    + "&response_type=code"
                    + "&scope=https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube"
                    + "&access_type=offline"
                    + "&prompt=consent"
                    + "&state=" + state;
        } else {
            authUrl = "https://www.tiktok.com/v2/auth/authorize/"
                    + "?client_key=" + configService.get("OAUTH_TIKTOK", "client_key")
                    + "&scope=video.publish,video.upload"
                    + "&response_type=code"
                    + "&redirect_uri=" + configService.get("OAUTH_TIKTOK", "redirect_uri")
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

        String traceId = TraceContext.getTraceId();
        log.info("OAuth callback received: platform={}, state={}, traceId={}", platform, state, traceId);

        String clientId = configService.get("OAUTH_YOUTUBE", "client_id");
        String clientSecret = configService.get("OAUTH_YOUTUBE", "client_secret");
        String redirectUri = configService.get("OAUTH_YOUTUBE", "redirect_uri");

        FormBody formBody = new FormBody.Builder()
                .add("code", code)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .build();

        Request request = new Request.Builder()
                .url(GOOGLE_TOKEN_URL)
                .post(formBody)
                .build();

        String accessToken;
        String refreshToken;
        long expiresIn;

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Google token exchange failed: status={}, body={}, traceId={}", response.code(), body, traceId);
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED,
                        "Google token exchange failed: HTTP " + response.code());
            }
            JsonNode json = objectMapper.readTree(body);
            accessToken = json.path("access_token").asText();
            refreshToken = json.has("refresh_token") ? json.path("refresh_token").asText() : null;
            expiresIn = json.path("expires_in").asLong(3600);
        } catch (IOException e) {
            log.error("Google token exchange error: traceId={}", traceId, e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED,
                    "Google token exchange error: " + e.getMessage(), e);
        }

        log.info("Google token exchange succeeded: traceId={}", traceId);

        String encryptedAccess = tokenEncryptService.encrypt(accessToken);
        String encryptedRefresh = refreshToken != null ? tokenEncryptService.encrypt(refreshToken) : null;

        PlatformAccount account = new PlatformAccount();
        account.setUserId(BizConstants.DEFAULT_USER_ID);
        account.setPlatform(PlatformType.fromCode(platform));
        account.setAccountName(pending.accountName);
        account.setAccessToken(encryptedAccess);
        account.setRefreshToken(encryptedRefresh);
        account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
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

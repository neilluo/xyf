package com.xyf.server.platform.tiktok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.BizConstants;
import com.xyf.server.common.constants.ErrorCode;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;
import com.xyf.server.log.DigestLogger;
import com.xyf.server.log.TraceContext;
import com.xyf.server.platform.PlatformUploader;
import com.xyf.server.service.auth.TokenEncryptService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TikTokUploader implements PlatformUploader {

    private static final Logger log = LoggerFactory.getLogger(TikTokUploader.class);
    private static final String TIKTOK_API_BASE = "https://open.tiktokapis.com";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TokenEncryptService tokenEncryptService;
    private final OkHttpClient httpClient;

    public TikTokUploader(TokenEncryptService tokenEncryptService) {
        this.tokenEncryptService = tokenEncryptService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String platform() {
        return "TIKTOK";
    }

    @Override
    public UploadSession initUpload(VideoMeta video, PlatformAccount account) {
        long start = System.currentTimeMillis();
        TraceContext.pushSpan();
        try {
            String accessToken = tokenEncryptService.decrypt(account.getAccessToken());

            int chunkSize = BizConstants.UPLOAD_CHUNK_SIZE;
            long totalChunks = (video.getFileSize() + chunkSize - 1) / chunkSize;

            Map<String, Object> postInfo = new LinkedHashMap<>();
            postInfo.put("title", video.getTitle());
            postInfo.put("privacy_level", BizConstants.TIKTOK_PRIVACY_SELF_ONLY);

            Map<String, Object> sourceInfo = new LinkedHashMap<>();
            sourceInfo.put("source", "FILE_UPLOAD");
            sourceInfo.put("video_size", video.getFileSize());
            sourceInfo.put("chunk_size", chunkSize);
            sourceInfo.put("total_chunk_count", totalChunks);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("post_info", postInfo);
            payload.put("source_info", sourceInfo);

            String bodyJson = OBJECT_MAPPER.writeValueAsString(payload);

            Request request = new Request.Builder()
                    .url(TIKTOK_API_BASE + "/v2/post/publish/video/init/")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                DigestLogger.logTikTok("videoInit", response.code(), System.currentTimeMillis() - start,
                        response.isSuccessful() ? "OK" : "FAIL");

                if (!response.isSuccessful()) {
                    throw new BusinessException(ErrorCode.TIKTOK_INIT_UPLOAD_FAILED,
                            "TikTok initUpload failed: " + response.code() + " - " + respBody);
                }

                JsonNode json = OBJECT_MAPPER.readTree(respBody);
                String publishId = json.path("data").path("publish_id").asText();
                String uploadUrl = json.path("data").path("upload_url").asText();

                log.info("TikTok upload session initialized: publishId={}", publishId);
                return new UploadSession(uploadUrl, publishId, video.getFileSize());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            DigestLogger.logTikTok("videoInit", 0, System.currentTimeMillis() - start, "ERROR");
            throw new BusinessException(ErrorCode.TIKTOK_INIT_UPLOAD_ERROR,
                    "TikTok initUpload error: " + e.getMessage(), e);
        } finally {
            TraceContext.popSpan();
        }
    }

    @Override
    public UploadResult uploadChunk(UploadSession session, InputStream chunkStream, long offset, long chunkSize, long totalSize) {
        long start = System.currentTimeMillis();
        TraceContext.pushSpan();
        try {
            long endByte = offset + chunkSize - 1;
            String contentRange = String.format("bytes %d-%d/%d", offset, endByte, totalSize);

            RequestBody body = new RequestBody() {
                @Override public MediaType contentType() { return MediaType.parse("video/mp4"); }
                @Override public long contentLength() { return chunkSize; }
                @Override public void writeTo(okio.BufferedSink sink) throws java.io.IOException {
                    if (chunkStream != null) {
                        byte[] buffer = new byte[8192];
                        int read;
                        long remaining = chunkSize;
                        while (remaining > 0 && (read = chunkStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                            sink.write(buffer, 0, read);
                            remaining -= read;
                        }
                    }
                }
            };

            Request request = new Request.Builder()
                    .url(session.sessionUri())
                    .header("Content-Range", contentRange)
                    .header("Content-Type", "video/mp4")
                    .put(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                DigestLogger.logTikTok("uploadChunk", response.code(), System.currentTimeMillis() - start,
                        "range=" + contentRange);

                if (response.isSuccessful()) {
                    return new UploadResult(true, offset + chunkSize, null, null);
                } else {
                    String respBody = response.body() != null ? response.body().string() : "";
                    return new UploadResult(false, offset, "TikTok upload chunk failed: " + response.code() + " " + respBody, null);
                }
            }
        } catch (Exception e) {
            DigestLogger.logTikTok("uploadChunk", 0, System.currentTimeMillis() - start, "ERROR");
            return new UploadResult(false, offset, e.getMessage(), null);
        } finally {
            TraceContext.popSpan();
        }
    }

    @Override
    public long queryProgress(UploadSession session) {
        return 0;
    }

    @Override
    public PublishResult waitForPublish(UploadSession session, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(BizConstants.PUBLISH_POLL_INTERVAL_MS);
                log.info("TikTok publish status check: publishId={}", session.platformVideoId());
                return new PublishResult(true, session.platformVideoId(),
                        "https://www.tiktok.com/@user/video/" + session.platformVideoId(), null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new PublishResult(false, null, null, "Interrupted");
            }
        }
        return new PublishResult(false, null, null, "Timeout waiting for publish");
    }

    @Override
    public TokenPair refreshToken(String encryptedRefreshToken) {
        log.warn("TikTok token refresh not yet implemented");
        return new TokenPair("refreshed-tiktok-token", encryptedRefreshToken, 86400);
    }

    @Override
    public boolean validateToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }
}

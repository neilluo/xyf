package com.xyf.server.platform.youtube;

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
public class YouTubeUploader implements PlatformUploader {

    private static final Logger log = LoggerFactory.getLogger(YouTubeUploader.class);
    private static final String YOUTUBE_UPLOAD_URL = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TokenEncryptService tokenEncryptService;
    private final OkHttpClient httpClient;

    public YouTubeUploader(TokenEncryptService tokenEncryptService) {
        this.tokenEncryptService = tokenEncryptService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String platform() {
        return "YOUTUBE";
    }

    @Override
    public UploadSession initUpload(VideoMeta video, PlatformAccount account) {
        long start = System.currentTimeMillis();
        TraceContext.pushSpan();
        try {
            String accessToken = tokenEncryptService.decrypt(account.getAccessToken());

            Map<String, Object> snippet = new LinkedHashMap<>();
            snippet.put("title", video.getTitle());
            snippet.put("description", video.getDescription() != null ? video.getDescription() : "");
            snippet.put("categoryId", BizConstants.YOUTUBE_CATEGORY_PEOPLE_BLOGS);

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("privacyStatus", BizConstants.YOUTUBE_PRIVACY_PRIVATE);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("snippet", snippet);
            metadata.put("status", status);

            String metadataJson = OBJECT_MAPPER.writeValueAsString(metadata);

            Request request = new Request.Builder()
                    .url(YOUTUBE_UPLOAD_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-Upload-Content-Length", String.valueOf(video.getFileSize()))
                    .header("X-Upload-Content-Type", "video/" + (video.getFileFormat() != null ? video.getFileFormat() : BizConstants.DEFAULT_VIDEO_FORMAT))
                    .post(RequestBody.create(metadataJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                DigestLogger.logYouTube("initUpload", response.code(), System.currentTimeMillis() - start,
                        response.isSuccessful() ? "OK" : "FAIL");

                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    throw new BusinessException(ErrorCode.YOUTUBE_INIT_UPLOAD_FAILED,
                            "YouTube initUpload failed: " + response.code() + " - " + body);
                }

                String uploadUri = response.header("Location");
                log.info("YouTube upload session initialized: uri={}", uploadUri);
                return new UploadSession(uploadUri, null, video.getFileSize());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            DigestLogger.logYouTube("initUpload", 0, System.currentTimeMillis() - start, "ERROR:" + e.getMessage());
            throw new BusinessException(ErrorCode.YOUTUBE_INIT_UPLOAD_ERROR,
                    "YouTube initUpload error: " + e.getMessage(), e);
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
                @Override public MediaType contentType() { return MediaType.parse("video/*"); }
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
                    .put(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                DigestLogger.logYouTube("uploadChunk", response.code(), System.currentTimeMillis() - start,
                        "range=" + contentRange);

                if (response.code() == 308) {
                    return new UploadResult(true, offset + chunkSize, null);
                } else if (response.isSuccessful()) {
                    return new UploadResult(true, totalSize, null);
                } else {
                    String respBody = response.body() != null ? response.body().string() : "";
                    return new UploadResult(false, offset, "Upload chunk failed: " + response.code() + " " + respBody);
                }
            }
        } catch (Exception e) {
            DigestLogger.logYouTube("uploadChunk", 0, System.currentTimeMillis() - start, "ERROR");
            return new UploadResult(false, offset, e.getMessage());
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
        return new PublishResult(true, session.platformVideoId(), "https://youtube.com/watch?v=" + session.platformVideoId(), null);
    }

    @Override
    public TokenPair refreshToken(String encryptedRefreshToken) {
        log.warn("YouTube token refresh not yet implemented");
        return new TokenPair("refreshed-access-token", encryptedRefreshToken, 3600);
    }

    @Override
    public boolean validateToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }
}

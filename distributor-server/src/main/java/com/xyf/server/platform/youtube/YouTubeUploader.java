package com.xyf.server.platform.youtube;

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
import java.util.concurrent.TimeUnit;

/**
 * YouTube Resumable Upload 实现
 * <p>
 * 使用 YouTube Data API v3 的 Resumable Upload 机制：
 * 1. POST 初始化上传 → 获取 upload URI
 * 2. PUT 分片上传 → 流式分片 (10MB chunks)
 * 3. 上传完成即为发布（YouTube 无需额外 publish 步骤）
 */
@Component
public class YouTubeUploader implements PlatformUploader {

    private static final Logger log = LoggerFactory.getLogger(YouTubeUploader.class);
    private static final String YOUTUBE_UPLOAD_URL = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB

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

            // 构建视频元数据 JSON
            String metadata = String.format("""
                {
                  "snippet": {
                    "title": "%s",
                    "description": "%s",
                    "categoryId": "22"
                  },
                  "status": {
                    "privacyStatus": "private"
                  }
                }
                """, escapeJson(video.getTitle()),
                    escapeJson(video.getDescription() != null ? video.getDescription() : ""));

            Request request = new Request.Builder()
                    .url(YOUTUBE_UPLOAD_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-Upload-Content-Length", String.valueOf(video.getFileSize()))
                    .header("X-Upload-Content-Type", "video/" + (video.getFileFormat() != null ? video.getFileFormat() : "mp4"))
                    .post(RequestBody.create(metadata, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                DigestLogger.logYouTube("initUpload", response.code(), System.currentTimeMillis() - start,
                        response.isSuccessful() ? "OK" : "FAIL");

                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    throw new RuntimeException("YouTube initUpload failed: " + response.code() + " - " + body);
                }

                String uploadUri = response.header("Location");
                log.info("YouTube upload session initialized: uri={}", uploadUri);
                return new UploadSession(uploadUri, null, video.getFileSize());
            }
        } catch (Exception e) {
            DigestLogger.logYouTube("initUpload", 0, System.currentTimeMillis() - start, "ERROR:" + e.getMessage());
            throw new RuntimeException("YouTube initUpload error: " + e.getMessage(), e);
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
                    // 308 Resume Incomplete - 还有更多 chunk
                    return new UploadResult(true, offset + chunkSize, null);
                } else if (response.isSuccessful()) {
                    // 200/201 = 上传完成
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
        // YouTube uses Content-Range header to query progress
        // For simplicity, return 0 (caller tracks via UploadResult)
        return 0;
    }

    @Override
    public PublishResult waitForPublish(UploadSession session, long timeoutMs) {
        // YouTube: upload completion = published (no extra step needed)
        return new PublishResult(true, session.platformVideoId(), "https://youtube.com/watch?v=" + session.platformVideoId(), null);
    }

    @Override
    public TokenPair refreshToken(String encryptedRefreshToken) {
        // TODO: Call Google OAuth token endpoint to refresh
        // POST https://oauth2.googleapis.com/token with refresh_token
        log.warn("YouTube token refresh not yet implemented");
        return new TokenPair("refreshed-access-token", encryptedRefreshToken, 3600);
    }

    @Override
    public boolean validateToken(String accessToken) {
        // Simple check: token is not null/empty
        return accessToken != null && !accessToken.isBlank();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

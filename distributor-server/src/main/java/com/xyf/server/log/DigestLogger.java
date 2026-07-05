package com.xyf.server.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DigestLogger - 外部调用摘要日志（pipe分隔格式）
 * <p>
 * 每次外部 API 调用（YouTube/TikTok）记录一行摘要：
 * traceId|rpcId|platform|api|statusCode|timeMs|result
 */
public class DigestLogger {

    private static final Logger YOUTUBE_DIGEST = LoggerFactory.getLogger("YOUTUBE_DIGEST");
    private static final Logger TIKTOK_DIGEST = LoggerFactory.getLogger("TIKTOK_DIGEST");

    /**
     * 记录 YouTube API 调用摘要
     */
    public static void logYouTube(String api, int statusCode, long timeMs, String result) {
        YOUTUBE_DIGEST.info("{}|{}|YouTube|{}|{}|{}ms|{}",
                TraceContext.getTraceId(),
                TraceContext.getRpcId(),
                api,
                statusCode,
                timeMs,
                result);
    }

    /**
     * 记录 TikTok API 调用摘要
     */
    public static void logTikTok(String api, int statusCode, long timeMs, String result) {
        TIKTOK_DIGEST.info("{}|{}|TikTok|{}|{}|{}ms|{}",
                TraceContext.getTraceId(),
                TraceContext.getRpcId(),
                api,
                statusCode,
                timeMs,
                result);
    }
}

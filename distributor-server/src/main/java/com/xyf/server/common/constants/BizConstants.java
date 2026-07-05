package com.xyf.server.common.constants;

public final class BizConstants {

    private BizConstants() {}

    public static final long DEFAULT_USER_ID = 1L;
    public static final int DEFAULT_MAX_RETRY = 3;
    public static final int UPLOAD_CHUNK_SIZE = 10 * 1024 * 1024;
    public static final int SCHEDULER_POLL_INTERVAL_MS = 10000;
    public static final int SCHEDULER_BATCH_SIZE = 10;
    public static final long PUBLISH_TIMEOUT_MS = 300000L;
    public static final int PUBLISH_POLL_INTERVAL_MS = 5000;
    public static final int PAGE_SIZE_MAX = 100;
    public static final int AUTH_CACHE_TTL_MINUTES = 30;
    public static final int DEFAULT_TOKEN_EXPIRE_HOURS = 1;
    public static final String DEFAULT_VIDEO_FORMAT = "mp4";

    // YouTube platform constants
    public static final String YOUTUBE_CATEGORY_PEOPLE_BLOGS = "22";
    public static final String YOUTUBE_PRIVACY_PRIVATE = "private";

    // TikTok platform constants
    public static final String TIKTOK_PRIVACY_SELF_ONLY = "SELF_ONLY";
}

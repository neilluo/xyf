package com.xyf.server.common.constants;

public final class ErrorCode {

    private ErrorCode() {}

    public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
    public static final String VIDEO_NOT_FOUND = "VIDEO_NOT_FOUND";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String INVALID_STATUS = "INVALID_STATUS";
    public static final String UNSUPPORTED_PLATFORM = "UNSUPPORTED_PLATFORM";
    public static final String INVALID_STATE = "INVALID_STATE";

    public static final String YOUTUBE_INIT_UPLOAD_FAILED = "YOUTUBE_INIT_UPLOAD_FAILED";
    public static final String YOUTUBE_INIT_UPLOAD_ERROR = "YOUTUBE_INIT_UPLOAD_ERROR";
    public static final String TIKTOK_INIT_UPLOAD_FAILED = "TIKTOK_INIT_UPLOAD_FAILED";
    public static final String TIKTOK_INIT_UPLOAD_ERROR = "TIKTOK_INIT_UPLOAD_ERROR";
    public static final String OSS_STS_TOKEN_FAILED = "OSS_STS_TOKEN_FAILED";
    public static final String OSS_CLIENT_NOT_INITIALIZED = "OSS_CLIENT_NOT_INITIALIZED";
    public static final String TOKEN_ENCRYPT_FAILED = "TOKEN_ENCRYPT_FAILED";
    public static final String TOKEN_DECRYPT_FAILED = "TOKEN_DECRYPT_FAILED";
}

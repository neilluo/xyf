package com.xyf.server.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {

    PENDING("PENDING"),
    UPLOADING("UPLOADING"),
    PROCESSING("PROCESSING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    TaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

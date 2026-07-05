package com.xyf.server.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlatformType {

    YOUTUBE("YOUTUBE"),
    TIKTOK("TIKTOK");

    @EnumValue
    @JsonValue
    private final String code;

    PlatformType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PlatformType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PlatformType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + code);
    }
}

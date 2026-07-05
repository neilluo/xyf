package com.xyf.server.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {

    ACTIVE("ACTIVE"),
    EXPIRED("EXPIRED"),
    REVOKED("REVOKED");

    @EnumValue
    @JsonValue
    private final String code;

    AccountStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

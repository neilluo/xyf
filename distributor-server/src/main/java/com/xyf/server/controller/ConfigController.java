package com.xyf.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xyf.server.common.ApiResponse;
import com.xyf.server.config.DynamicConfigService;
import com.xyf.server.domain.SystemConfig;
import com.xyf.server.mapper.SystemConfigMapper;
import com.xyf.server.service.auth.TokenEncryptService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configs")
public class ConfigController {

    private final SystemConfigMapper configMapper;
    private final DynamicConfigService dynamicConfigService;
    private final TokenEncryptService encryptService;

    public ConfigController(SystemConfigMapper configMapper,
                            DynamicConfigService dynamicConfigService,
                            TokenEncryptService encryptService) {
        this.configMapper = configMapper;
        this.dynamicConfigService = dynamicConfigService;
        this.encryptService = encryptService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<SystemConfig> all = configMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>(all.size());
        for (SystemConfig c : all) {
            Map<String, Object> item = new HashMap<>(8);
            item.put("group", c.getConfigGroup());
            item.put("key", c.getConfigKey());
            item.put("value", maskValue(c.getConfigValue(), c.getEncrypted()));
            item.put("encrypted", c.getEncrypted());
            item.put("description", c.getDescription());
            item.put("updatedAt", c.getUpdatedAt());
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    @PutMapping("/{group}/{key}")
    public ApiResponse<Void> update(@PathVariable String group,
                                    @PathVariable String key,
                                    @RequestBody UpdateConfigRequest body) {
        String valueToStore = body.getValue();
        int encrypted = 0;
        if (body.getEncrypted() != null && body.getEncrypted()) {
            valueToStore = encryptService.encrypt(valueToStore);
            encrypted = 1;
        }

        LambdaUpdateWrapper<SystemConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, group)
                .eq(SystemConfig::getConfigKey, key)
                .set(SystemConfig::getConfigValue, valueToStore)
                .set(SystemConfig::getEncrypted, encrypted);

        int rows = configMapper.update(null, wrapper);
        if (rows == 0) {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigGroup(group);
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(valueToStore);
            newConfig.setEncrypted(encrypted);
            configMapper.insert(newConfig);
        }

        dynamicConfigService.reload();
        return ApiResponse.ok();
    }

    @PostMapping("/reload")
    public ApiResponse<Void> reload() {
        dynamicConfigService.reload();
        return ApiResponse.ok();
    }

    private String maskValue(String value, Integer encrypted) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (encrypted != null && encrypted == 1) {
            return "****";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }

    public static class UpdateConfigRequest {
        private String value;
        private Boolean encrypted;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public Boolean getEncrypted() { return encrypted; }
        public void setEncrypted(Boolean encrypted) { this.encrypted = encrypted; }
    }
}

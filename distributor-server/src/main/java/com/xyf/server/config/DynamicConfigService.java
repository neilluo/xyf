package com.xyf.server.config;

import com.xyf.server.domain.SystemConfig;
import com.xyf.server.mapper.SystemConfigMapper;
import com.xyf.server.service.auth.TokenEncryptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicConfigService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamicConfigService.class);
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(64);
    private final SystemConfigMapper configMapper;
    private final TokenEncryptService encryptService;

    public DynamicConfigService(SystemConfigMapper configMapper, TokenEncryptService encryptService) {
        this.configMapper = configMapper;
        this.encryptService = encryptService;
    }

    @Override
    public void run(ApplicationArguments args) {
        reload();
        log.info("DynamicConfig loaded, {} entries in cache", cache.size());
    }

    public void reload() {
        List<SystemConfig> all = configMapper.selectList(null);
        ConcurrentHashMap<String, String> newCache = new ConcurrentHashMap<>(all.size());
        for (SystemConfig c : all) {
            String value = c.getConfigValue();
            if (value != null && Integer.valueOf(1).equals(c.getEncrypted())) {
                try {
                    value = encryptService.decrypt(value);
                } catch (Exception e) {
                    log.warn("Failed to decrypt config {}.{}, using raw value", c.getConfigGroup(), c.getConfigKey());
                }
            }
            newCache.put(c.getConfigGroup() + "." + c.getConfigKey(), value != null ? value : "");
        }
        cache.clear();
        cache.putAll(newCache);
    }

    public String get(String group, String key) {
        return cache.get(group + "." + key);
    }

    public String get(String group, String key, String defaultValue) {
        return cache.getOrDefault(group + "." + key, defaultValue);
    }

    public Map<String, String> getGroup(String group) {
        String prefix = group + ".";
        Map<String, String> result = new HashMap<>();
        cache.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), v);
            }
        });
        return result;
    }
}

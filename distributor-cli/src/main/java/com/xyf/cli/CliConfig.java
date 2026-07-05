package com.xyf.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CLI 配置管理 - 读写 ~/.vd/config.yaml
 */
public class CliConfig {

    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".vd");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.yaml");

    private String serverUrl;
    private String apiKey;

    public static CliConfig load() {
        CliConfig config = new CliConfig();
        if (Files.exists(CONFIG_FILE)) {
            try {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(Files.newInputStream(CONFIG_FILE));
                if (data != null) {
                    Map<String, Object> server = getMap(data, "server");
                    if (server != null) {
                        config.serverUrl = (String) server.get("url");
                        config.apiKey = (String) server.get("api-key");
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: Failed to read config: " + e.getMessage());
            }
        }
        return config;
    }

    public void save() throws IOException {
        Files.createDirectories(CONFIG_DIR);

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", serverUrl);
        server.put("api-key", apiKey);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("server", server);

        Yaml yaml = new Yaml();
        String content = yaml.dump(root);
        Files.writeString(CONFIG_FILE, content);

        // 设置权限 600（仅所有者读写）
        try {
            Files.setPosixFilePermissions(CONFIG_FILE, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows 不支持 POSIX 权限
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public boolean isConfigured() {
        return serverUrl != null && !serverUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }
}

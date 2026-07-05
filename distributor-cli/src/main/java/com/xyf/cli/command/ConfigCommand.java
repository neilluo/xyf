package com.xyf.cli.command;

import com.xyf.cli.CliConfig;
import picocli.CommandLine.Command;

import java.util.Scanner;

/**
 * 配置管理命令
 */
@Command(name = "config", description = "配置管理")
public class ConfigCommand implements Runnable {

    @Command(name = "init", description = "交互式初始化配置")
    public void init() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入服务端地址 (如 https://vd.yourdomain.com): ");
        String url = scanner.nextLine().trim();

        System.out.print("请输入 API Key: ");
        String apiKey = scanner.nextLine().trim();

        try {
            CliConfig config = new CliConfig();
            config.setServerUrl(url);
            config.setApiKey(apiKey);
            config.save();
            System.out.println("✅ 配置完成！保存到 ~/.vd/config.yaml");
        } catch (Exception e) {
            System.err.println("❌ 保存配置失败: " + e.getMessage());
        }
    }

    @Command(name = "show", description = "查看当前配置")
    public void show() {
        CliConfig config = CliConfig.load();
        System.out.println("服务端地址: " + (config.getServerUrl() != null ? config.getServerUrl() : "(未配置)"));
        System.out.println("API Key: " + (config.getApiKey() != null ? maskKey(config.getApiKey()) : "(未配置)"));
    }

    @Override
    public void run() {
        show();
    }

    private String maskKey(String key) {
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}

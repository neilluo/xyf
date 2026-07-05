package com.xyf.cli.command;

import com.xyf.cli.CliConfig;
import com.xyf.cli.http.HttpClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * 任务管理命令
 */
@Command(name = "task", description = "分发任务管理")
public class TaskCommand implements Runnable {

    @Override
    public void run() {
        list();
    }

    @Command(name = "list", description = "列出所有任务")
    public void list() {
        CliConfig config = CliConfig.load();
        if (!config.isConfigured()) { System.err.println("❌ 请先运行 vd config init"); return; }
        HttpClient client = new HttpClient(config.getServerUrl(), config.getApiKey());
        try {
            String response = client.get("/api/v1/tasks?page=1&size=20");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("❌ " + e.getMessage());
        }
    }

    @Command(name = "status", description = "查看任务状态")
    public void status(@Parameters(index = "0", description = "任务 ID") Long taskId) {
        CliConfig config = CliConfig.load();
        if (!config.isConfigured()) { System.err.println("❌ 请先运行 vd config init"); return; }
        HttpClient client = new HttpClient(config.getServerUrl(), config.getApiKey());
        try {
            String response = client.get("/api/v1/tasks/" + taskId);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("❌ " + e.getMessage());
        }
    }

    @Command(name = "retry", description = "重试失败的任务")
    public void retry(@Parameters(index = "0", description = "任务 ID") Long taskId) {
        CliConfig config = CliConfig.load();
        if (!config.isConfigured()) { System.err.println("❌ 请先运行 vd config init"); return; }
        HttpClient client = new HttpClient(config.getServerUrl(), config.getApiKey());
        try {
            String response = client.postEmpty("/api/v1/tasks/" + taskId + "/retry");
            System.out.println("✅ 重试已触发: " + response);
        } catch (Exception e) {
            System.err.println("❌ " + e.getMessage());
        }
    }
}

package com.xyf.cli.command;

import com.xyf.cli.CliConfig;
import com.xyf.cli.http.HttpClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频发布命令
 */
@Command(name = "publish", description = "发布视频到目标平台")
public class PublishCommand implements Runnable {

    @Parameters(index = "0", description = "视频 ID")
    private Long videoId;

    @Option(names = "--platform", required = true, description = "目标平台（逗号分隔：youtube,tiktok）")
    private String platforms;

    @Option(names = "--account-id", required = true, description = "账号 ID")
    private Long accountId;

    @Option(names = "--no-wait", description = "不等待结果")
    private boolean noWait;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        if (!config.isConfigured()) {
            System.err.println("❌ 请先运行 vd config init");
            return;
        }

        HttpClient client = new HttpClient(config.getServerUrl(), config.getApiKey());

        try {
            for (String platform : platforms.split(",")) {
                platform = platform.trim().toUpperCase();
                System.out.println("⚡ 创建分发任务: video=" + videoId + " → " + platform);

                Map<String, Object> body = new HashMap<>();
                body.put("videoId", videoId);
                body.put("platform", platform);
                body.put("accountId", accountId);

                String response = client.post("/api/v1/tasks", body);
                System.out.println("✅ 任务已创建: " + response);
            }

            if (!noWait) {
                System.out.println("\n📊 任务已提交，使用 vd task list 查看进度");
            }
        } catch (Exception e) {
            System.err.println("❌ 发布失败: " + e.getMessage());
        }
    }
}

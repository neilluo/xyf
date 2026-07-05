package com.xyf.cli.command;

import com.xyf.cli.CliConfig;
import com.xyf.cli.http.HttpClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 视频上传命令
 */
@Command(name = "upload", description = "上传视频到 OSS")
public class UploadCommand implements Runnable {

    @Parameters(index = "0", description = "视频文件路径")
    private File videoFile;

    @Option(names = "--title", required = true, description = "视频标题")
    private String title;

    @Option(names = "--description", description = "视频描述")
    private String description;

    @Option(names = "--tags", description = "标签（逗号分隔）")
    private String tags;

    @Option(names = "--publish", description = "上传后立即分发到指定平台（逗号分隔）")
    private String publishTo;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        if (!config.isConfigured()) {
            System.err.println("❌ 请先运行 vd config init 配置服务端地址和 API Key");
            return;
        }

        if (!videoFile.exists()) {
            System.err.println("❌ 文件不存在: " + videoFile.getAbsolutePath());
            return;
        }

        HttpClient client = new HttpClient(config.getServerUrl(), config.getApiKey());

        try {
            System.out.println("📤 正在上传视频: " + videoFile.getName());
            System.out.println("   标题: " + title);

            // 1. 注册视频元数据（实际应先上传到 OSS，这里简化）
            Map<String, Object> videoMeta = new HashMap<>();
            videoMeta.put("title", title);
            videoMeta.put("description", description != null ? description : "");
            videoMeta.put("ossBucket", "video-dist-sg");
            videoMeta.put("ossKey", "videos/" + videoFile.getName());
            videoMeta.put("ossRegion", "ap-southeast-1");
            videoMeta.put("fileSize", videoFile.length());
            videoMeta.put("fileFormat", getFileExtension(videoFile.getName()));

            if (tags != null) {
                videoMeta.put("tags", tags.split(","));
            }

            String response = client.post("/api/v1/videos", videoMeta);
            System.out.println("✅ 视频注册成功!");
            System.out.println("   响应: " + response);

        } catch (Exception e) {
            System.err.println("❌ 上传失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "mp4";
    }
}

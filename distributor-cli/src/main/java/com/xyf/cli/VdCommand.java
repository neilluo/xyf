package com.xyf.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * 视频分发平台 CLI 主命令
 * <p>
 * 用法: vd <command> [options]
 */
@Command(
    name = "vd",
    mixinStandardHelpOptions = true,
    version = "vd 1.0.0",
    description = "视频分发平台 CLI - 上传视频并分发到 YouTube/TikTok",
    subcommands = {CommandLine.HelpCommand.class}
)
public class VdCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new VdCommand()).execute(args);
        System.exit(exitCode);
    }
}

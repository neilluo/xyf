package com.xyf.server.log;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EagleEye 风格 30 位 traceId 生成器
 * <p>
 * 格式：IP(8位hex) + 时间戳(13位) + 自增(4位) + PID(2位hex) + 随机(3位hex)
 * 示例：0a1b2c3d1719821234567000100ab1
 */
public class EagleEyeIdGenerator {

    private static final String IP_HEX;
    private static final String PID_HEX;
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    static {
        IP_HEX = getLocalIpHex();
        PID_HEX = getPidHex();
    }

    /**
     * 生成 30 位 traceId
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(30);
        sb.append(IP_HEX);                                          // 8位
        sb.append(System.currentTimeMillis());                      // 13位
        sb.append(String.format("%04d", COUNTER.incrementAndGet() % 10000)); // 4位
        sb.append(PID_HEX);                                         // 2位
        sb.append(String.format("%03x", (int) (Math.random() * 4096))); // 3位
        return sb.toString();
    }

    private static String getLocalIpHex() {
        try {
            byte[] bytes = InetAddress.getLocalHost().getAddress();
            return String.format("%02x%02x%02x%02x", bytes[0] & 0xFF, bytes[1] & 0xFF, bytes[2] & 0xFF, bytes[3] & 0xFF);
        } catch (Exception e) {
            return "00000000";
        }
    }

    private static String getPidHex() {
        long pid = ProcessHandle.current().pid();
        return String.format("%02x", (int) (pid % 256));
    }
}

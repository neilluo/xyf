package com.xyf.server.log;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class EagleEyeIdGenerator {

    private static final String IP_HEX;
    private static final String PID_HEX;
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    static {
        IP_HEX = getLocalIpHex();
        PID_HEX = getPidHex();
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(30);
        sb.append(IP_HEX);
        sb.append(System.currentTimeMillis());
        sb.append(String.format("%04d", COUNTER.incrementAndGet() % 10000));
        sb.append(PID_HEX);
        sb.append(String.format("%03x", ThreadLocalRandom.current().nextInt(4096)));
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

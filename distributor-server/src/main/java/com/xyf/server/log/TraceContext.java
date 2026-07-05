package com.xyf.server.log;

import org.slf4j.MDC;

/**
 * TraceContext - 管理 MDC 中的 traceId 和 rpcId
 * <p>
 * 支持 rpcId 层级（如 0.1, 0.1.1, 0.1.2）用于追踪调用链
 */
public class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String RPC_ID = "rpcId";

    private static final ThreadLocal<int[]> SPAN_COUNTER = ThreadLocal.withInitial(() -> new int[]{0});

    /**
     * 初始化 trace 上下文（入口调用）
     */
    public static String initTrace() {
        String traceId = EagleEyeIdGenerator.generate();
        MDC.put(TRACE_ID, traceId);
        MDC.put(RPC_ID, "0");
        SPAN_COUNTER.set(new int[]{0});
        return traceId;
    }

    /**
     * 设置已有的 traceId（透传场景）
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
        MDC.put(RPC_ID, "0");
    }

    /**
     * 获取当前 traceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取当前 rpcId
     */
    public static String getRpcId() {
        return MDC.get(RPC_ID);
    }

    /**
     * 进入子 span（增加 rpcId 层级）
     */
    public static String pushSpan() {
        int[] counter = SPAN_COUNTER.get();
        counter[0]++;
        String currentRpc = MDC.get(RPC_ID);
        String newRpc = (currentRpc != null ? currentRpc : "0") + "." + counter[0];
        MDC.put(RPC_ID, newRpc);
        return newRpc;
    }

    /**
     * 退出子 span
     */
    public static void popSpan() {
        String rpc = MDC.get(RPC_ID);
        if (rpc != null && rpc.contains(".")) {
            String parent = rpc.substring(0, rpc.lastIndexOf('.'));
            MDC.put(RPC_ID, parent);
        }
    }

    /**
     * 清理 trace 上下文
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(RPC_ID);
        SPAN_COUNTER.remove();
    }
}

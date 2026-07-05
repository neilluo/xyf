package com.xyf.server.log;

import org.slf4j.MDC;

public class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String RPC_ID = "rpcId";

    private static final ThreadLocal<int[]> SPAN_COUNTER = ThreadLocal.withInitial(() -> new int[]{0});

    public static String initTrace() {
        String traceId = EagleEyeIdGenerator.generate();
        MDC.put(TRACE_ID, traceId);
        MDC.put(RPC_ID, "0");
        SPAN_COUNTER.set(new int[]{0});
        return traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
        MDC.put(RPC_ID, "0");
        SPAN_COUNTER.set(new int[]{0});
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static String getRpcId() {
        return MDC.get(RPC_ID);
    }

    public static String pushSpan() {
        int[] counter = SPAN_COUNTER.get();
        counter[0]++;
        String currentRpc = MDC.get(RPC_ID);
        String newRpc = (currentRpc != null ? currentRpc : "0") + "." + counter[0];
        MDC.put(RPC_ID, newRpc);
        return newRpc;
    }

    public static void popSpan() {
        String rpc = MDC.get(RPC_ID);
        if (rpc != null && rpc.contains(".")) {
            String parent = rpc.substring(0, rpc.lastIndexOf('.'));
            MDC.put(RPC_ID, parent);
        }
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(RPC_ID);
        SPAN_COUNTER.remove();
    }
}

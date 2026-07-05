package com.xyf.server.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * EagleEye Filter - 为每个 HTTP 请求自动生成 traceId 并注入 MDC
 * <p>
 * 优先级最高，确保所有后续 Filter/Interceptor/Controller 都能获取 traceId
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EagleEyeFilter extends OncePerRequestFilter {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 优先使用请求头中的 traceId（客户端透传）
            String traceId = request.getHeader(HEADER_TRACE_ID);
            if (traceId != null && !traceId.isBlank()) {
                TraceContext.setTraceId(traceId);
            } else {
                traceId = TraceContext.initTrace();
            }

            // 写入响应头，方便客户端关联
            response.setHeader(HEADER_TRACE_ID, traceId);

            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}

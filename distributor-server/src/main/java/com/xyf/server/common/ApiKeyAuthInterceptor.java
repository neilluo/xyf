package com.xyf.server.common;

import com.xyf.server.config.DynamicConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final DynamicConfigService configService;

    public ApiKeyAuthInterceptor(DynamicConfigService configService) {
        this.configService = configService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminApiKey = configService.get("SECURITY", "admin_api_key");
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!adminApiKey.equals(token)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_API_KEY", "Invalid API key");
            return false;
        }

        return true;
    }

    private void sendError(HttpServletResponse response, int status, String code, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}", code, message)
        );
    }
}

package com.xyf.server.config;

import com.xyf.server.common.ApiKeyAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    public WebConfig(ApiKeyAuthInterceptor apiKeyAuthInterceptor) {
        this.apiKeyAuthInterceptor = apiKeyAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/api/v1/auth/*/callback",     // OAuth 回调
                        "/api/v1/auth/*/authorize-url"  // OAuth 授权入口（浏览器直接访问）
                );
    }
}

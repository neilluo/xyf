package com.xyf.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 启动测试 - 验证应用能正常加载上下文
 */
@SpringBootTest
@ActiveProfiles("test")
class DistributorServerApplicationTest {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常加载
    }
}

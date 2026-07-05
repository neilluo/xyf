package com.xyf.server.controller;

import com.xyf.server.common.ApiResponse;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.service.auth.OAuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OAuth 授权 REST API
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OAuthService oAuthService;

    public AuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    /** 获取 OAuth 授权 URL */
    @GetMapping("/{platform}/authorize-url")
    public ApiResponse<Map<String, String>> getAuthorizeUrl(
            @PathVariable String platform,
            @RequestParam(defaultValue = "default") String accountName) {
        return ApiResponse.ok(oAuthService.getAuthorizeUrl(platform, accountName));
    }

    /** OAuth 回调（浏览器重定向） */
    @GetMapping("/{platform}/callback")
    public String handleCallback(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestParam String state) {
        oAuthService.handleCallback(platform, code, state);
        return "<html><body><h2>✅ 授权成功！</h2><p>请返回 CLI 终端。</p></body></html>";
    }

    /** CLI 轮询授权状态 */
    @GetMapping("/{platform}/status")
    public ApiResponse<Map<String, Object>> getAuthStatus(
            @PathVariable String platform,
            @RequestParam String state) {
        return ApiResponse.ok(oAuthService.getAuthStatus(state));
    }

    /** 列出授权账号 */
    @GetMapping("/accounts")
    public ApiResponse<List<PlatformAccount>> listAccounts() {
        return ApiResponse.ok(oAuthService.listAccounts());
    }
}

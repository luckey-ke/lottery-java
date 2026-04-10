package com.lottery.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 鉴权拦截器
 * <p>
 * 公开接口（GET /status, /analyze, /trend）无需鉴权。
 * 写操作（POST /fetch）和推荐接口需携带 Authorization: Bearer {token}。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${lottery.auth.token:}")
    private String configuredToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 未配置 token 则不启用鉴权
        if (configuredToken == null || configuredToken.isBlank()) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // GET 请求中的公开接口放行
        if ("GET".equals(method) && isPublicEndpoint(path)) {
            return true;
        }

        // 校验 Bearer Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (configuredToken.equals(token)) {
                return true;
            }
        }

        log.warn("[鉴权拒绝] {} {} - token 缺失或无效", method, path);
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授权，请在请求头中携带有效的 Authorization: Bearer {token}\"}");
        return false;
    }

    private boolean isPublicEndpoint(String path) {
        return path.endsWith("/status")
                || path.endsWith("/results")
                || path.endsWith("/latest")
                || path.contains("/analyze")
                || path.contains("/trend")
                || path.contains("/recommend/history")
                || path.contains("/recommend/stats")
                || path.contains("/fetch/history")
                || path.contains("/fetch/tasks/");
    }
}

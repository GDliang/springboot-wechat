// [file name]: AdminInterceptor.java
package com.goldenegg.interceptor;

import com.goldenegg.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    // 简单的Token存储（实际应该用Redis等）
    private Map<String, Object> tokenStore = new ConcurrentHashMap<>();

    @Autowired
    private AdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 检查是否是管理接口
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/api/admin")) {
            return true; // 非管理接口直接放行
        }

        // 获取Token
        String token = request.getHeader("Admin-Token");
        if (token == null) {
            token = request.getParameter("adminToken");
        }

        // Token验证逻辑
        if (token != null && tokenStore.containsKey(token)) {
            // Token有效，允许访问
            return true;
        }

        // Token无效，返回401
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"管理员认证失败\"}");
        return false;
    }

    // 添加Token
    public void addToken(String token, Object adminInfo) {
        tokenStore.put(token, adminInfo);
    }

    // 移除Token
    public void removeToken(String token) {
        tokenStore.remove(token);
    }

    // 验证Token
    public boolean validateToken(String token) {
        return tokenStore.containsKey(token);
    }
}
package com.hotel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 简单档登录过滤器：token 使用 Base64("userId:role")。
 * 这里使用轻量过滤器，避免项目定位回到中等档安全框架。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isWhitelisted(request)) {
            filterChain.doFilter(stripIdentityHeaders(request, null, null), response);
            return;
        }

        String token = request.getHeader("Authorization");
        try {
            String[] parts = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split(":");
            String role = "管理员".equals(parts[1]) ? "ADMIN" : "USER";
            filterChain.doFilter(stripIdentityHeaders(request, parts[0], role), response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.equals("/api/user/register") || path.equals("/api/user/login")) {
            return true;
        }
        return HttpMethod.GET.matches(request.getMethod())
                && (path.startsWith("/api/hotel") || path.startsWith("/api/review/hotel"));
    }

    private HttpServletRequest stripIdentityHeaders(HttpServletRequest request, String userId, String role) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) {
                    return userId;
                }
                if ("X-User-Role".equalsIgnoreCase(name)) {
                    return role;
                }
                if ("X-User-Id".equalsIgnoreCase(name) || "X-User-Role".equalsIgnoreCase(name)) {
                    return null;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                String header = getHeader(name);
                if (header == null) {
                    return Collections.emptyEnumeration();
                }
                return Collections.enumeration(List.of(header));
            }
        };
    }
}

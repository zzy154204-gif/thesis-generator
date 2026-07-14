package com.example.thesisgenerator.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 放行登录、注册、Swagger、静态资源
        if (path.startsWith("/api/v1/auth/") || path.contains("/doc.html") ||
                path.contains("/v3/api-docs") || path.contains("/swagger") ||
                path.contains("/webjars")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (!jwtUtil.isTokenExpired(token)) {
                    Claims claims = jwtUtil.parseToken(token);
                    request.setAttribute("userId", claims.get("userId", Long.class));
                    request.setAttribute("username", claims.get("username", String.class));
                    request.setAttribute("role", claims.get("role", String.class));
                }
            } catch (Exception ignored) {
            }
        }

        chain.doFilter(request, response);
    }
}

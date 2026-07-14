package com.example.thesisgenerator.config;

import com.example.thesisgenerator.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {

        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        RoleRequired annotation = hm.getMethodAnnotation(RoleRequired.class);
        if (annotation == null) {
            annotation = hm.getBeanType().getAnnotation(RoleRequired.class);
        }
        if (annotation == null) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        if (role == null) {
            throw new BusinessException(401, "请先登录");
        }

        List<String> allowed = Arrays.asList(annotation.value());
        if (!allowed.contains(role)) {
            throw new BusinessException(403, "权限不足，需要角色: " + String.join(", ", allowed));
        }

        return true;
    }
}

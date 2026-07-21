package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.dto.LoginRequest;
import com.example.thesisgenerator.dto.LoginResponse;
import com.example.thesisgenerator.dto.RegisterRequest;
import com.example.thesisgenerator.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        authService.logout(userId);
        return Result.ok();
    }

    /**
     * 更新个人信息
     * PUT /api/v1/auth/profile
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request,
                                   @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        authService.updateProfile(userId, body.get("realName"));
        return Result.ok();
    }

    /**
     * 修改密码
     * PUT /api/v1/auth/password
     */
    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }
}

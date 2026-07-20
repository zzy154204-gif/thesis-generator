package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.dto.LoginRequest;
import com.example.thesisgenerator.dto.LoginResponse;
import com.example.thesisgenerator.dto.RegisterRequest;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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
        if (userId != null) {
            authService.logout(userId);
        }
        return Result.ok();
    }

    /**
     * 获取当前用户信息
     * GET /api/v1/auth/profile
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getProfile(userId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("role", user.getRole());
        profile.put("collegeId", user.getCollegeId());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        return Result.ok(profile);
    }

    /**
     * 修改个人信息
     * PUT /api/v1/auth/profile
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(HttpServletRequest request,
                                      @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String realName = (String) body.get("realName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        return Result.ok(authService.updateProfile(userId, realName, email, phone));
    }

    /**
     * 修改密码
     * PUT /api/v1/auth/password
     */
    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.ok();
    }
}

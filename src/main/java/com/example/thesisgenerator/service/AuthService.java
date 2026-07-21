package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.config.JwtUtil;
import com.example.thesisgenerator.dto.LoginRequest;
import com.example.thesisgenerator.dto.LoginResponse;
import com.example.thesisgenerator.dto.RegisterRequest;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!user.getEnabled()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getRealName());

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        if (!"STUDENT".equals(request.getRole()) && !"TEACHER".equals(request.getRole())) {
            throw new BusinessException(400, "不允许注册管理员账号");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setCollegeId(request.getCollegeId());
        user.setEnabled(true);

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getRealName());

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    public void logout(Long userId) {
        // Token 无状态 JWT，前端清除即可
    }

    /**
     * 更新个人信息
     */
    public void updateProfile(Long userId, String realName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (realName != null && !realName.isBlank()) {
            user.setRealName(realName);
        }
        userRepository.save(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码长度不少于6位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

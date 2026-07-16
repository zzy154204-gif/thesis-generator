package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.config.JwtUtil;
import com.example.thesisgenerator.dto.LoginRequest;
import com.example.thesisgenerator.dto.LoginResponse;
import com.example.thesisgenerator.dto.RegisterRequest;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!user.getEnabled()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Token 缓存到 Redis（如果可用）
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    "token:" + user.getId(),
                    token,
                    Duration.ofHours(24)
            );
        }

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 仅允许注册学生和教师
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

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Token 缓存到 Redis（如果可用）
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    "token:" + user.getId(),
                    token,
                    Duration.ofHours(24)
            );
        }

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    public void logout(Long userId) {
        if (redisTemplate != null) {
            redisTemplate.delete("token:" + userId);
        }
    }
}

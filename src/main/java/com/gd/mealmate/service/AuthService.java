package com.gd.mealmate.service;

import com.gd.mealmate.config.JwtConfig;
import com.gd.mealmate.dto.request.LoginRequest;
import com.gd.mealmate.dto.request.RegisterRequest;
import com.gd.mealmate.dto.response.AuthResponse;
import com.gd.mealmate.dto.response.UserDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.UserMapper;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.repository.UserRepository;
import com.gd.mealmate.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, userMapper.toDto(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, userMapper.toDto(user));
    }
}

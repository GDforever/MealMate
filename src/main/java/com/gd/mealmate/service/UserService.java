package com.gd.mealmate.service;

import com.gd.mealmate.dto.request.UpdatePreferencesRequest;
import com.gd.mealmate.dto.response.UserDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.UserMapper;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updatePreferences(Long userId, UpdatePreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.setTastePreferences(request.getTastePreferences());
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }
}

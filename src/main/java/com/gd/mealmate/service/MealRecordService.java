package com.gd.mealmate.service;

import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.MealRecordMapper;
import com.gd.mealmate.model.entity.MealRecord;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.model.enums.MealType;
import com.gd.mealmate.repository.MealRecordRepository;
import com.gd.mealmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealRecordService {

    private final MealRecordRepository mealRecordRepository;
    private final UserRepository userRepository;
    private final MealRecordMapper mealRecordMapper;

    @Transactional
    public MealRecordDto createMealRecord(Long userId, MealRecordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        MealRecord mealRecord = mealRecordMapper.toEntity(request);
        mealRecord.setUser(user);
        mealRecord = mealRecordRepository.save(mealRecord);

        return mealRecordMapper.toDto(mealRecord);
    }

    public Page<MealRecordDto> getMealRecords(Long userId, LocalDateTime startDate, LocalDateTime endDate, MealType mealType, Pageable pageable) {
        return mealRecordRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordedAt"), endDate));
            }
            if (mealType != null) {
                predicates.add(cb.equal(root.get("mealType"), mealType));
            }
            query.orderBy(cb.desc(root.get("recordedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(mealRecordMapper::toDto);
    }

    public MealRecordDto getMealRecordById(Long userId, Long recordId) {
        MealRecord mealRecord = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用餐记录不存在"));

        if (!mealRecord.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "无权访问该用餐记录");
        }

        return mealRecordMapper.toDto(mealRecord);
    }

    @Transactional
    public MealRecordDto updateMealRecord(Long userId, Long recordId, MealRecordRequest request) {
        MealRecord mealRecord = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用餐记录不存在"));

        if (!mealRecord.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "无权修改该用餐记录");
        }

        mealRecord.setMealType(request.getMealType());
        mealRecord.setFoodName(request.getFoodName());
        mealRecord.setRestaurantName(request.getRestaurantName());
        mealRecord.setLocation(request.getLocation());
        mealRecord.setLatitude(request.getLatitude());
        mealRecord.setLongitude(request.getLongitude());
        mealRecord.setRecordedAt(request.getRecordedAt());
        mealRecord.setUserRating(request.getUserRating());
        mealRecord.setTags(request.getTags());

        mealRecord = mealRecordRepository.save(mealRecord);

        return mealRecordMapper.toDto(mealRecord);
    }

    @Transactional
    public void deleteMealRecord(Long userId, Long recordId) {
        MealRecord mealRecord = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用餐记录不存在"));

        if (!mealRecord.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "无权删除该用餐记录");
        }

        mealRecordRepository.delete(mealRecord);
    }
}

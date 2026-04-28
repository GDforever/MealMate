package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.MealRecord;
import com.gd.mealmate.model.enums.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long>, JpaSpecificationExecutor<MealRecord> {
    Page<MealRecord> findByUserIdOrderByRecordedAtDesc(Long userId, Pageable pageable);

    List<MealRecord> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}

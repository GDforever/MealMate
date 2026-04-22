package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.MealRecord;
import com.gd.mealmate.model.enums.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {
    Page<MealRecord> findByUserIdOrderByRecordedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT m FROM MealRecord m WHERE m.user.id = :userId " +
           "AND (:startDate IS NULL OR m.recordedAt >= :startDate) " +
           "AND (:endDate IS NULL OR m.recordedAt <= :endDate) " +
           "AND (:mealType IS NULL OR m.mealType = :mealType) " +
           "ORDER BY m.recordedAt DESC")
    Page<MealRecord> findByFilters(@Param("userId") Long userId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    @Param("mealType") MealType mealType,
                                    Pageable pageable);

    List<MealRecord> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}

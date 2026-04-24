package com.gd.mealmate.model.entity;

import com.gd.mealmate.model.enums.MealType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private String foodName;

    @Column(length = 200)
    private String restaurantName;

    @Column(length = 500)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    private Integer userRating;

    @Column(length = 1000)
    private String tags;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

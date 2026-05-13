package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByExternalId(String externalId);

    @Query("SELECT r FROM Restaurant r WHERE " +
           "(:latitude IS NULL OR :longitude IS NULL OR " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(r.latitude)) * " +
           "cos(radians(r.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(r.latitude)))) < :radius/1000) " +
           "AND (:cuisineType IS NULL OR r.cuisineType = :cuisineType) " +
           "AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CAST(CONCAT('%', :keyword, '%') AS string)))")
    Page<Restaurant> findNearby(@Param("latitude") Double latitude,
                                 @Param("longitude") Double longitude,
                                 @Param("radius") Double radius,
                                 @Param("cuisineType") String cuisineType,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}

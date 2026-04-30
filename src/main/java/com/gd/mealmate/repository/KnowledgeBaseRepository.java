package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    @Query("SELECT kb FROM KnowledgeBase kb WHERE " +
           "kb.user.id = :userId OR kb.isPublic = true " +
           "ORDER BY kb.updatedAt DESC")
    Page<KnowledgeBase> findByUserAccessible(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE " +
           "(kb.user.id = :userId OR kb.isPublic = true) " +
           "AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(kb.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY kb.updatedAt DESC")
    Page<KnowledgeBase> findByUserAccessibleAndKeyword(@Param("userId") Long userId,
                                                       @Param("keyword") String keyword,
                                                       Pageable pageable);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.user.id = :userId ORDER BY kb.updatedAt DESC")
    List<KnowledgeBase> findByUserId(@Param("userId") Long userId);
}

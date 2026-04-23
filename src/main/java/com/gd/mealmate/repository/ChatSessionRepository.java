package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.ChatSession;
import com.gd.mealmate.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);

    boolean existsByUserAndId(User user, Long sessionId);
}

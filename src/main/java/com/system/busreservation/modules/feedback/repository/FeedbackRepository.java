package com.system.busreservation.modules.feedback.repository;

import com.system.busreservation.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUser_UserId(Long userId);

    Optional<Feedback> findByFeedbackIdAndUser_UserId(Long feedbackId, Long userId);

    void deleteByUser_UserId(Long userId);

    List<Feedback> findByType(String type);

    @Query("SELECT f FROM Feedback f WHERE f.reply IS NOT NULL AND TRIM(f.reply) != ''")
    List<Feedback> findWithReplies();

    @Query("SELECT f FROM Feedback f WHERE f.reply IS NULL OR TRIM(f.reply) = ''")
    List<Feedback> findWithoutReplies();

    List<Feedback> findByUser_UserIdAndType(Long userId, String type);

    long countByType(String type);

    long countByUser_UserId(Long userId);
}

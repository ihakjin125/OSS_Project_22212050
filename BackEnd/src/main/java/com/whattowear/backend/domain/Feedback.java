package com.whattowear.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Getter @Setter
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Integer score;
    private Double tempAtTime;
    private LocalDateTime createdAt;
}

interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByMemberId(Long memberId);
}
package com.example.study_cards.domain.ai.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_generation_logs", indexes = {
        @Index(name = "idx_ai_generation_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_ai_generation_logs_type", columnList = "type"),
        @Index(name = "idx_ai_generation_logs_created_at", columnList = "createdAt")
})
public class AiGenerationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiGenerationType type;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(length = 100)
    private String model;

    private Integer cardsGenerated;

    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder
    public AiGenerationLog(User user, AiGenerationType type, String prompt, String response,
                           String model, Integer cardsGenerated, Boolean success, String errorMessage) {
        this.user = user;
        this.type = type;
        this.prompt = prompt;
        this.response = response;
        this.model = model;
        this.cardsGenerated = cardsGenerated;
        this.success = success != null ? success : true;
        this.errorMessage = errorMessage;
    }
}

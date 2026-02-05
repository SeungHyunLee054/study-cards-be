package com.example.study_cards.domain.generation.entity;

public enum GenerationStatus {
    PENDING,    // 검토 대기
    APPROVED,   // 승인됨
    REJECTED,   // 거부됨
    MIGRATED    // Card로 이동 완료
}

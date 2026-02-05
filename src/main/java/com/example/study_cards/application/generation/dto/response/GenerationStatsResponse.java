package com.example.study_cards.application.generation.dto.response;

import java.util.List;

public record GenerationStatsResponse(
        List<ModelStats> byModel,
        OverallStats overall
) {
    public record ModelStats(
            String model,
            long totalGenerated,
            long approved,
            long rejected,
            long pending,
            long migrated,
            double approvalRate
    ) {
        public static ModelStats of(String model, long total, long approved, long rejected, long pending, long migrated) {
            double rate = total > 0 ? (approved * 100.0) / (approved + rejected) : 0.0;
            return new ModelStats(model, total, approved, rejected, pending, migrated, Math.round(rate * 10) / 10.0);
        }
    }

    public record OverallStats(
            long totalGenerated,
            long approved,
            long rejected,
            long pending,
            long migrated,
            double approvalRate
    ) {
        public static OverallStats of(long total, long approved, long rejected, long pending, long migrated) {
            double rate = total > 0 ? (approved * 100.0) / (approved + rejected) : 0.0;
            return new OverallStats(total, approved, rejected, pending, migrated, Math.round(rate * 10) / 10.0);
        }
    }

    public static GenerationStatsResponse of(List<ModelStats> modelStats, OverallStats overallStats) {
        return new GenerationStatsResponse(modelStats, overallStats);
    }
}

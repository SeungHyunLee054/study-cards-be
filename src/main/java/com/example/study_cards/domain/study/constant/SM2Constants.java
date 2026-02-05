package com.example.study_cards.domain.study.constant;

/**
 * SM-2 (SuperMemo 2) 알고리즘에서 사용되는 상수들
 *
 * SM-2 알고리즘은 간격 반복 학습을 위한 알고리즘으로,
 * E-Factor(이지니스 팩터)를 기반으로 복습 간격을 계산합니다.
 */
public final class SM2Constants {

    private SM2Constants() {
    }

    /**
     * E-Factor 계산에 사용되는 기본 상수
     * delta = BASE_DELTA - (5 - quality) * (QUALITY_MULTIPLIER + (5 - quality) * QUALITY_SQUARED_MULTIPLIER)
     */
    public static final double BASE_DELTA = 0.1;

    /**
     * 품질 점수에 곱해지는 계수
     */
    public static final double QUALITY_MULTIPLIER = 0.08;

    /**
     * 품질 점수의 제곱에 곱해지는 계수
     */
    public static final double QUALITY_SQUARED_MULTIPLIER = 0.02;

    /**
     * E-Factor의 최소값
     * 이 값 아래로 내려가지 않도록 제한합니다.
     */
    public static final double MIN_EF_FACTOR = 1.3;

    /**
     * 정답일 때의 품질 점수 (0-5 스케일에서 4)
     */
    public static final int QUALITY_CORRECT = 4;

    /**
     * 오답일 때의 품질 점수 (0-5 스케일에서 2)
     */
    public static final int QUALITY_INCORRECT = 2;

    /**
     * 품질 점수 최대값 (SM-2 알고리즘에서 5로 고정)
     */
    public static final int MAX_QUALITY = 5;

    /**
     * 첫 번째 복습 간격 (일)
     */
    public static final int FIRST_INTERVAL = 1;

    /**
     * 두 번째 복습 간격 (일)
     */
    public static final int SECOND_INTERVAL = 6;

    /**
     * E-Factor 델타 계산
     *
     * @param quality 품질 점수 (0-5)
     * @return 델타 값
     */
    public static double calculateDelta(int quality) {
        return BASE_DELTA - (MAX_QUALITY - quality) * (QUALITY_MULTIPLIER + (MAX_QUALITY - quality) * QUALITY_SQUARED_MULTIPLIER);
    }

    /**
     * 새로운 E-Factor 계산
     *
     * @param currentEfFactor 현재 E-Factor
     * @param isCorrect 정답 여부
     * @return 새로운 E-Factor (최소값 이상 보장)
     */
    public static double calculateNewEfFactor(double currentEfFactor, boolean isCorrect) {
        int quality = isCorrect ? QUALITY_CORRECT : QUALITY_INCORRECT;
        double delta = calculateDelta(quality);
        return Math.max(currentEfFactor + delta, MIN_EF_FACTOR);
    }
}

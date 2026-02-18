package com.example.study_cards.application.ai.prompt;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.common.util.AiCategoryType;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;

import java.util.function.Function;
import java.util.function.Supplier;

public final class AiPromptTemplateFactory {

    private AiPromptTemplateFactory() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static String buildPrompt(GenerateUserCardRequest request, Category category) {
        return buildByCategoryType(
                category.getCode(),
                categoryCode -> buildUserJlptPrompt(request, categoryCode),
                () -> buildUserToeicPrompt(request),
                () -> buildUserCsPrompt(request),
                () -> buildUserGenericPrompt(request, category)
        );
    }

    public static String buildPrompt(Card sourceCard, Category category) {
        return buildByCategoryType(
                category.getCode(),
                categoryCode -> buildAdminJlptPrompt(sourceCard, categoryCode),
                () -> buildAdminToeicPrompt(sourceCard),
                () -> buildAdminCsPrompt(sourceCard),
                () -> buildAdminGenericPrompt(sourceCard, category)
        );
    }

    private static String buildByCategoryType(
            String categoryCode,
            Function<String, String> jlptPromptBuilder,
            Supplier<String> toeicPromptBuilder,
            Supplier<String> csPromptBuilder,
            Supplier<String> genericPromptBuilder
    ) {
        AiCategoryType categoryType = AiCategoryType.fromCode(categoryCode);
        return switch (categoryType) {
            case JLPT -> jlptPromptBuilder.apply(categoryCode);
            case TOEIC -> toeicPromptBuilder.get();
            case CS -> csPromptBuilder.get();
            case GENERIC -> genericPromptBuilder.get();
        };
    }

    private static String buildUserCsPrompt(GenerateUserCardRequest request) {
        String difficulty = defaultDifficulty(request.difficulty());
        return String.format("""
                당신은 컴퓨터 공학(CS) 튜터입니다.
                아래 텍스트를 분석하여 %d개의 학습 카드를 생성하세요.

                입력 텍스트:
                %s

                요구사항:
                1. 자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스 등 CS 핵심 개념을 질문-답변 형식으로 변환
                2. 난이도: %s
                3. 각 카드는 독립적으로 이해 가능해야 함
                4. 답변은 간결하지만 정확하게 작성
                5. 한글로 작성

                %s
                """,
                request.count(),
                request.sourceText(),
                difficulty,
                userJsonArrayOutputFormat()
        );
    }

    private static String buildUserToeicPrompt(GenerateUserCardRequest request) {
        String difficulty = defaultDifficulty(request.difficulty());
        return String.format("""
                당신은 TOEIC 학습 코치입니다.
                아래 텍스트를 분석하여 %d개의 학습 카드를 생성하세요.

                입력 텍스트:
                %s

                요구사항:
                1. 어휘, 문법, 문장 패턴을 중심으로 질문-답변 카드 생성
                2. 난이도: %s
                3. 토익 실전에서 자주 나오는 포인트를 반영
                4. 해설은 한국어로 작성

                %s
                """,
                request.count(),
                request.sourceText(),
                difficulty,
                userJsonArrayOutputFormat()
        );
    }

    private static String buildUserJlptPrompt(GenerateUserCardRequest request, String categoryCode) {
        String difficulty = defaultDifficulty(request.difficulty());
        String jlptLevel = AiCategoryType.toJlptLevel(categoryCode);
        return String.format("""
                당신은 JLPT %s 학습 코치입니다.
                아래 텍스트를 분석하여 %d개의 학습 카드를 생성하세요.

                입력 텍스트:
                %s

                요구사항:
                1. 어휘, 문형, 문법 포인트를 질문-답변 카드로 생성
                2. 난이도: %s
                3. 일본어 원문이 포함되면 핵심 표현을 questionSub에 보조 설명
                4. 해설은 한국어로 작성

                %s
                """,
                jlptLevel,
                request.count(),
                request.sourceText(),
                difficulty,
                userJsonArrayOutputFormat()
        );
    }

    private static String buildUserGenericPrompt(GenerateUserCardRequest request, Category category) {
        String difficulty = defaultDifficulty(request.difficulty());
        return String.format("""
                당신은 학습 전문가입니다.
                아래 텍스트를 분석하여 %d개의 학습 카드를 생성하세요.

                카테고리: %s
                입력 텍스트:
                %s

                요구사항:
                1. 핵심 개념을 질문-답변 형식으로 변환
                2. 난이도: %s
                3. 각 카드는 독립적으로 이해 가능해야 함
                4. 한글로 작성

                %s
                """,
                request.count(),
                category.getName(),
                request.sourceText(),
                difficulty,
                userJsonArrayOutputFormat()
        );
    }

    private static String userJsonArrayOutputFormat() {
        return """
                출력 형식 (JSON 배열만 출력, 다른 텍스트 없이):
                [
                  {
                    "question": "질문",
                    "questionSub": "부가 설명 (선택, 없으면 null)",
                    "answer": "답변",
                    "answerSub": "부가 정보 (선택, 없으면 null)"
                  }
                ]
                """;
    }

    private static String buildAdminToeicPrompt(Card sourceCard) {
        String outputFormat = adminQuizOutputFormat(
                "answer는 정답 알파벳(A, B, C, D 중 하나)이어야 합니다.",
                "예문 (빈칸 포함)",
                "\"A\", \"B\", \"C\", \"D\"",
                "정답 알파벳",
                "간단한 해설"
        );
        return """
            당신은 TOEIC 출제 전문가입니다.
            아래 단어/문장을 사용하여 TOEIC Part 5 스타일의 문제를 생성하세요.

            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 실제 비즈니스/일상 상황의 예문을 작성하세요.
            2. 4개의 선택지를 만드세요 (정답 1개 + 오답 3개).
            3. 오답은 비슷한 형태의 단어로 구성하세요 (품사 변형, 유사어 등).
            4. 난이도는 중급으로 맞추세요.

            %s
            """.formatted(sourceCard.getQuestion(), sourceCard.getAnswer(), outputFormat);
    }

    private static String buildAdminJlptPrompt(Card sourceCard, String categoryCode) {
        String jlptLevel = AiCategoryType.toJlptLevel(categoryCode);
        String outputFormat = adminQuizOutputFormat(
                "answer는 정답 번호(\"1\", \"2\", \"3\", \"4\" 중 하나)여야 합니다.",
                "예문 (빈칸 포함)",
                "\"1\", \"2\", \"3\", \"4\"",
                "정답 번호",
                "간단한 해설 (한국어)"
        );
        return """
            당신은 JLPT 출제 전문가입니다.
            아래 단어/문장을 사용하여 JLPT %s 스타일의 문제를 생성하세요.

            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 자연스러운 일본어 예문을 작성하세요.
            2. 4개의 선택지를 만드세요 (정답 1개 + 오답 3개).
            3. 오답은 문맥상 헷갈릴 수 있는 단어로 구성하세요.
            4. 난이도는 %s 레벨에 맞게 조정하세요.
            5. 해설은 반드시 한국어로 작성하세요.

            %s
            """.formatted(jlptLevel, sourceCard.getQuestion(), sourceCard.getAnswer(), jlptLevel, outputFormat);
    }

    private static String buildAdminCsPrompt(Card sourceCard) {
        return """
            당신은 컴퓨터 공학(CS) 교육 전문가입니다.
            아래 참고 질문/답변을 기반으로 새로운 학습 카드를 생성하세요.

            참고 질문: %s
            참고 답변: %s

            요구사항:
            1. 참고 내용과 관련되지만, 다른 관점의 새로운 질문을 작성하세요.
            2. 질문과 답변은 반드시 컴퓨터 공학 개념(자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스 등)과 직접 관련되어야 합니다.
            3. 한글로 작성하세요.
            4. 답변은 명확하고 이해하기 쉽게, 강의하듯이 작성하세요.

            출력 형식:
            - JSON만 정확히 출력하고, 다른 설명 문장은 절대 쓰지 마세요.
            - key 이름은 반드시 question, answer 두 개만 사용하세요. (소문자)
            - 다음 형식을 정확히 지키세요.

            {
              "question": "질문 내용",
              "answer": "답변 내용"
            }
            """.formatted(sourceCard.getQuestion(), sourceCard.getAnswer());
    }

    private static String buildAdminGenericPrompt(Card sourceCard, Category category) {
        String outputFormat = adminQuizOutputFormat(
                "answer는 정답 알파벳(A, B, C, D 중 하나)이어야 합니다.",
                "문제",
                "\"A\", \"B\", \"C\", \"D\"",
                "정답 알파벳",
                "간단한 해설"
        );
        return """
            아래 학습 자료를 기반으로 4지선다 문제를 생성하세요.

            카테고리: %s
            원본 질문: %s
            원본 답변: %s

            요구사항:
            1. 원본 내용을 기반으로 새로운 관점의 문제를 작성하세요.
            2. 4개의 선택지를 만드세요 (정답 1개 + 오답 3개).
            3. 오답은 그럴듯하지만 틀린 내용으로 구성하세요.
            4. 한글로 작성하세요.

            %s
            """.formatted(category.getName(), sourceCard.getQuestion(), sourceCard.getAnswer(), outputFormat);
    }

    private static String adminQuizOutputFormat(
            String answerRule,
            String questionExample,
            String optionExamples,
            String answerExample,
            String explanationExample
    ) {
        return """
                출력 형식:
                - JSON만 정확히 출력하고, 다른 설명 문장은 절대 쓰지 마세요.
                - key 이름은 반드시 question, options, answer, explanation 네 개만 사용하세요. (소문자)
                - options는 정확히 4개의 문자열 배열이어야 합니다.
                - %s
                - 다음 형식을 정확히 지키세요.

                {
                  "question": "%s",
                  "options": [%s],
                  "answer": "%s",
                  "explanation": "%s"
                }
                """.formatted(answerRule, questionExample, optionExamples, answerExample, explanationExample);
    }

    private static String defaultDifficulty(String difficulty) {
        return difficulty != null ? difficulty : "보통";
    }
}

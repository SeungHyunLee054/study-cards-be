package com.example.study_cards.application.ai.prompt;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptTemplateFactoryTest extends BaseUnitTest {

    @Nested
    @DisplayName("buildPrompt(request, category)")
    class BuildUserPromptTest {

        @Test
        @DisplayName("CS 카테고리는 CS 프롬프트를 생성한다")
        void buildPrompt_csCategory_returnsCsPrompt() {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "운영체제와 스케줄링 내용을 정리해줘",
                    "CS",
                    3,
                    "쉬움"
            );
            Category category = createCategory("CS", "컴퓨터 과학");

            String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

            assertThat(prompt).contains("컴퓨터 공학(CS) 튜터");
            assertThat(prompt).contains("\"question\"");
            assertThat(prompt).contains("\"answer\"");
        }

        @Test
        @DisplayName("TOEIC 카테고리는 TOEIC 프롬프트를 생성한다")
        void buildPrompt_toeicCategory_returnsToeicPrompt() {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "TOEIC part 5 grammar points",
                    "TOEIC",
                    2,
                    "보통"
            );
            Category category = createCategory("TOEIC", "영어 > TOEIC");

            String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

            assertThat(prompt).contains("TOEIC 학습 코치");
            assertThat(prompt).contains("토익 실전");
        }

        @Test
        @DisplayName("JLPT 카테고리는 레벨 기반 프롬프트를 생성한다")
        void buildPrompt_jlptCategory_returnsJlptPrompt() {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "日本語の文法ポイントをまとめて",
                    "JN_N3",
                    2,
                    "어려움"
            );
            Category category = createCategory("JN_N3", "일본어 > JLPT > N3");

            String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

            assertThat(prompt).contains("JLPT N3 학습 코치");
            assertThat(prompt).contains("어휘, 문형, 문법");
        }

        @Test
        @DisplayName("기타 카테고리는 Generic 프롬프트를 생성한다")
        void buildPrompt_genericCategory_returnsGenericPrompt() {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "경제학 수요와 공급의 기본 개념",
                    "MISC_GENERAL",
                    4,
                    "보통"
            );
            Category category = createCategory("MISC_GENERAL", "기타");

            String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

            assertThat(prompt).contains("학습 전문가");
            assertThat(prompt).contains("카테고리: 기타");
        }

        @Test
        @DisplayName("난이도 미지정 시 기본 난이도 보통을 사용한다")
        void buildPrompt_nullDifficulty_usesDefaultDifficulty() {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "운영체제 정리",
                    "CS",
                    1,
                    null
            );
            Category category = createCategory("CS", "컴퓨터 과학");

            String prompt = AiPromptTemplateFactory.buildPrompt(request, category);

            assertThat(prompt).contains("난이도: 보통");
        }
    }

    @Nested
    @DisplayName("buildPrompt(card, category)")
    class BuildAdminPromptTest {

        @Test
        @DisplayName("CS 카테고리는 관리자 CS 프롬프트를 생성한다")
        void buildPrompt_adminCsCategory_returnsCsPrompt() {
            Card sourceCard = createCard("운영체제란?", "자원 관리 소프트웨어");
            Category category = createCategory("CS", "컴퓨터 과학");

            String prompt = AiPromptTemplateFactory.buildPrompt(sourceCard, category);

            assertThat(prompt).contains("컴퓨터 공학(CS) 교육 전문가");
            assertThat(prompt).contains("\"question\"");
            assertThat(prompt).contains("\"answer\"");
        }

        @Test
        @DisplayName("TOEIC 카테고리는 관리자 TOEIC 프롬프트를 생성한다")
        void buildPrompt_adminToeicCategory_returnsToeicPrompt() {
            Card sourceCard = createCard("abandon", "포기하다");
            Category category = createCategory("TOEIC", "영어 > TOEIC");

            String prompt = AiPromptTemplateFactory.buildPrompt(sourceCard, category);

            assertThat(prompt).contains("TOEIC 출제 전문가");
            assertThat(prompt).contains("\"options\"");
            assertThat(prompt).contains("\"explanation\"");
        }

        @Test
        @DisplayName("JLPT 카테고리는 관리자 JLPT 프롬프트를 생성한다")
        void buildPrompt_adminJlptCategory_returnsJlptPrompt() {
            Card sourceCard = createCard("問題", "문제");
            Category category = createCategory("JN_N2", "일본어 > JLPT > N2");

            String prompt = AiPromptTemplateFactory.buildPrompt(sourceCard, category);

            assertThat(prompt).contains("JLPT N2 스타일");
            assertThat(prompt).contains("\"answer\": \"정답 번호\"");
        }

        @Test
        @DisplayName("기타 카테고리는 관리자 Generic 프롬프트를 생성한다")
        void buildPrompt_adminGenericCategory_returnsGenericPrompt() {
            Card sourceCard = createCard("수요 법칙", "가격이 오르면 수요량은 감소");
            Category category = createCategory("MISC_GENERAL", "기타");

            String prompt = AiPromptTemplateFactory.buildPrompt(sourceCard, category);

            assertThat(prompt).contains("아래 학습 자료를 기반으로 4지선다 문제를 생성하세요.");
            assertThat(prompt).contains("카테고리: 기타");
        }
    }

    private Category createCategory(String code, String name) {
        return Category.builder()
                .code(code)
                .name(name)
                .displayOrder(1)
                .build();
    }

    private Card createCard(String question, String answer) {
        Category category = createCategory("CS", "컴퓨터 과학");
        return Card.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .build();
    }
}

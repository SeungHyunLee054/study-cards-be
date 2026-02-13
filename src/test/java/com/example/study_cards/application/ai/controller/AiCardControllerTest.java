package com.example.study_cards.application.ai.controller;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class AiCardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private AiGenerationService aiGenerationService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);
        verifyUserEmail("test@example.com");

        SignInRequest signInRequest = new SignInRequest("test@example.com", "password123");
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        categoryRepository.save(Category.builder()
                .code("CS")
                .name("Computer Science")
                .build());
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("POST /api/ai/generate-cards")
    class GenerateCardsTest {

        @Test
        @DisplayName("AI 카드를 생성한다")
        void generateCards_success() throws Exception {
            String aiResponse = """
                    [
                      {
                        "question": "운영체제란 무엇인가?",
                        "questionSub": null,
                        "answer": "컴퓨터 하드웨어와 소프트웨어 자원을 관리하는 시스템 소프트웨어",
                        "answerSub": null
                      }
                    ]
                    """;
            given(aiGenerationService.generateContent(anyString())).willReturn(aiResponse);
            given(aiGenerationService.getDefaultModel()).willReturn("gpt-4");

            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "운영체제는 컴퓨터 하드웨어와 소프트웨어 자원을 관리하는 시스템 소프트웨어입니다.",
                    "CS",
                    1,
                    "보통"
            );

            mockMvc.perform(post("/api/ai/generate-cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.generatedCards").isArray())
                    .andExpect(jsonPath("$.generatedCards.length()").value(1))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.remainingLimit").isNumber())
                    .andDo(document("ai/generate-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("sourceText").type(JsonFieldType.STRING).description("AI 생성을 위한 입력 텍스트"),
                                    fieldWithPath("categoryCode").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("count").type(JsonFieldType.NUMBER).description("생성할 카드 수 (1~20)"),
                                    fieldWithPath("difficulty").type(JsonFieldType.STRING).description("난이도").optional()
                            ),
                            responseFields(
                                    fieldWithPath("generatedCards").type(JsonFieldType.ARRAY).description("생성된 카드 목록"),
                                    fieldWithPath("generatedCards[].id").type(JsonFieldType.NUMBER).description("카드 ID"),
                                    fieldWithPath("generatedCards[].question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("generatedCards[].questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("generatedCards[].answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("generatedCards[].answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("generatedCards[].categoryCode").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("generatedCards[].aiGenerated").type(JsonFieldType.BOOLEAN).description("AI 생성 여부"),
                                    fieldWithPath("count").type(JsonFieldType.NUMBER).description("생성된 카드 수"),
                                    fieldWithPath("remainingLimit").type(JsonFieldType.NUMBER).description("남은 생성 한도")
                            )
                    ));
        }

        @Test
        @DisplayName("유효성 검증 실패 시 400을 반환한다")
        void generateCards_validationFail_returns400() throws Exception {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "",
                    "CS",
                    1,
                    "보통"
            );

            mockMvc.perform(post("/api/ai/generate-cards")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void generateCards_unauthorized_returns401() throws Exception {
            GenerateUserCardRequest request = new GenerateUserCardRequest(
                    "운영체제에 대한 학습 카드를 생성해주세요",
                    "CS",
                    1,
                    "보통"
            );

            mockMvc.perform(post("/api/ai/generate-cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/ai/generation-limit")
    class GetGenerationLimitTest {

        @Test
        @DisplayName("AI 생성 한도를 조회한다")
        void getGenerationLimit_success() throws Exception {
            mockMvc.perform(get("/api/ai/generation-limit")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.limit").isNumber())
                    .andExpect(jsonPath("$.used").isNumber())
                    .andExpect(jsonPath("$.remaining").isNumber())
                    .andExpect(jsonPath("$.isLifetime").isBoolean())
                    .andDo(document("ai/get-generation-limit",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("limit").type(JsonFieldType.NUMBER).description("총 한도"),
                                    fieldWithPath("used").type(JsonFieldType.NUMBER).description("사용량"),
                                    fieldWithPath("remaining").type(JsonFieldType.NUMBER).description("남은 한도"),
                                    fieldWithPath("isLifetime").type(JsonFieldType.BOOLEAN).description("평생 한도 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getGenerationLimit_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/ai/generation-limit"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

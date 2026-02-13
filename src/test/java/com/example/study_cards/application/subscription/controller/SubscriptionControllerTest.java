package com.example.study_cards.application.subscription.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

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
class SubscriptionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;

    @BeforeEach
    void setUp() {
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
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/subscriptions/plans")
    class GetPlansTest {

        @Test
        @DisplayName("구독 플랜 목록을 조회한다")
        void getPlans_success() throws Exception {
            mockMvc.perform(get("/api/subscriptions/plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].plan").exists())
                    .andExpect(jsonPath("$[0].displayName").exists())
                    .andExpect(jsonPath("$[0].monthlyPrice").isNumber())
                    .andExpect(jsonPath("$[0].yearlyPrice").isNumber())
                    .andDo(document("subscription/get-plans",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("[].plan").type(JsonFieldType.STRING).description("플랜 코드 (FREE, PRO)"),
                                    fieldWithPath("[].displayName").type(JsonFieldType.STRING).description("플랜 표시명"),
                                    fieldWithPath("[].monthlyPrice").type(JsonFieldType.NUMBER).description("월간 요금"),
                                    fieldWithPath("[].yearlyPrice").type(JsonFieldType.NUMBER).description("연간 요금"),
                                    fieldWithPath("[].canGenerateAiCards").type(JsonFieldType.BOOLEAN).description("AI 카드 생성 가능 여부"),
                                    fieldWithPath("[].canUseAiRecommendations").type(JsonFieldType.BOOLEAN).description("AI 복습 추천 사용 가능 여부"),
                                    fieldWithPath("[].aiGenerationDailyLimit").type(JsonFieldType.NUMBER).description("AI 생성 일일 제한"),
                                    fieldWithPath("[].isPurchasable").type(JsonFieldType.BOOLEAN).description("구매 가능 여부")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/subscriptions/me")
    class GetMySubscriptionTest {

        @Test
        @DisplayName("구독이 없으면 204 No Content를 반환한다")
        void getMySubscription_noSubscription_returns204() throws Exception {
            mockMvc.perform(get("/api/subscriptions/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("subscription/get-my-subscription-empty",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getMySubscription_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/subscriptions/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/subscriptions/cancel")
    class CancelSubscriptionTest {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void cancelSubscription_unauthorized_returns401() throws Exception {
            mockMvc.perform(post("/api/subscriptions/cancel")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}

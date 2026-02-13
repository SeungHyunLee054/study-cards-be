package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.payment.dto.request.CheckoutRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
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
class PaymentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private TossPaymentService tossPaymentService;

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
    @DisplayName("POST /api/payments/checkout")
    class CheckoutTest {

        @Test
        @DisplayName("체크아웃 성공 시 주문 정보를 반환한다")
        void checkout_success() throws Exception {
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();

            mockMvc.perform(post("/api/payments/checkout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.customerKey").exists())
                    .andExpect(jsonPath("$.amount").value(9900))
                    .andExpect(jsonPath("$.orderName").exists())
                    .andDo(document("payment/checkout",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("plan").type(JsonFieldType.STRING).description("구독 플랜 (PRO)"),
                                    fieldWithPath("billingCycle").type(JsonFieldType.STRING).description("결제 주기 (MONTHLY, YEARLY)")
                            ),
                            responseFields(
                                    fieldWithPath("orderId").type(JsonFieldType.STRING).description("주문 ID"),
                                    fieldWithPath("customerKey").type(JsonFieldType.STRING).description("고객 키"),
                                    fieldWithPath("amount").type(JsonFieldType.NUMBER).description("결제 금액"),
                                    fieldWithPath("orderName").type(JsonFieldType.STRING).description("주문명")
                            )
                    ));
        }

        @Test
        @DisplayName("플랜이 없으면 400을 반환한다")
        void checkout_nullPlan_returns400() throws Exception {
            String invalidRequest = """
                    {
                        "billingCycle": "MONTHLY"
                    }
                    """;

            mockMvc.perform(post("/api/payments/checkout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("결제 주기가 없으면 400을 반환한다")
        void checkout_nullBillingCycle_returns400() throws Exception {
            String invalidRequest = """
                    {
                        "plan": "PRO"
                    }
                    """;

            mockMvc.perform(post("/api/payments/checkout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void checkout_unauthorized_returns401() throws Exception {
            CheckoutRequest request = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();

            mockMvc.perform(post("/api/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/payments/confirm")
    class ConfirmPaymentTest {

        @Test
        @DisplayName("결제 확인 성공 시 구독 정보를 반환한다")
        void confirmPayment_success() throws Exception {
            CheckoutRequest checkoutRequest = fixtureMonkey.giveMeBuilder(CheckoutRequest.class)
                    .set("plan", SubscriptionPlan.PRO)
                    .set("billingCycle", BillingCycle.MONTHLY)
                    .sample();
            String checkoutResponseBody = mockMvc.perform(post("/api/payments/checkout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checkoutRequest)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String orderId = objectMapper.readTree(checkoutResponseBody).get("orderId").asText();
            int amount = objectMapper.readTree(checkoutResponseBody).get("amount").asInt();

            TossConfirmResponse tossResponse = createMockTossResponse();
            given(tossPaymentService.confirmPayment(anyString(), anyString(), anyInt()))
                    .willReturn(tossResponse);

            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", "test_payment_key_123")
                    .set("orderId", orderId)
                    .set("amount", amount)
                    .sample();

            mockMvc.perform(post("/api/payments/confirm")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.plan").value("PRO"))
                    .andExpect(jsonPath("$.status").exists())
                    .andDo(document("payment/confirm-payment",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("paymentKey").type(JsonFieldType.STRING).description("토스 결제 키"),
                                    fieldWithPath("orderId").type(JsonFieldType.STRING).description("주문 ID"),
                                    fieldWithPath("amount").type(JsonFieldType.NUMBER).description("결제 금액")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("구독 ID"),
                                    fieldWithPath("plan").type(JsonFieldType.STRING).description("구독 플랜"),
                                    fieldWithPath("planDisplayName").type(JsonFieldType.STRING).description("플랜 표시명"),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description("구독 상태"),
                                    fieldWithPath("billingCycle").type(JsonFieldType.STRING).description("결제 주기"),
                                    fieldWithPath("startDate").type(JsonFieldType.STRING).description("시작일"),
                                    fieldWithPath("endDate").type(JsonFieldType.STRING).description("종료일"),
                                    fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("활성화 여부"),
                                    fieldWithPath("canGenerateAiCards").type(JsonFieldType.BOOLEAN).description("AI 카드 생성 가능 여부"),
                                    fieldWithPath("canUseAiRecommendations").type(JsonFieldType.BOOLEAN).description("AI 복습 추천 사용 가능 여부"),
                                    fieldWithPath("aiGenerationDailyLimit").type(JsonFieldType.NUMBER).description("AI 생성 일일 제한")
                            )
                    ));
        }

        @Test
        @DisplayName("paymentKey가 비어있으면 400을 반환한다")
        void confirmPayment_blankPaymentKey_returns400() throws Exception {
            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", "")
                    .set("orderId", "ORDER_12345")
                    .set("amount", 3900)
                    .sample();

            mockMvc.perform(post("/api/payments/confirm")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount가 음수면 400을 반환한다")
        void confirmPayment_negativeAmount_returns400() throws Exception {
            ConfirmPaymentRequest request = fixtureMonkey.giveMeBuilder(ConfirmPaymentRequest.class)
                    .set("paymentKey", "payment_key")
                    .set("orderId", "ORDER_12345")
                    .set("amount", -100)
                    .sample();

            mockMvc.perform(post("/api/payments/confirm")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/payments/invoices")
    class GetPaymentHistoryTest {

        @Test
        @DisplayName("결제 내역을 조회한다")
        void getPaymentHistory_success() throws Exception {
            mockMvc.perform(get("/api/payments/invoices")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andDo(document("payment/get-invoices",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("결제 내역 목록"),
                                    fieldWithPath("pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                    fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 여부"),
                                    fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 아님 여부"),
                                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                    fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("number").type(JsonFieldType.NUMBER).description("페이지 번호"),
                                    fieldWithPath("sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                    fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                    fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void getPaymentHistory_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/payments/invoices"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private TossConfirmResponse createMockTossResponse() {
        return new TossConfirmResponse(
                "test_payment_key_123",
                "ORDER_12345",
                "orderName",
                "DONE",
                3900,
                "카드",
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString(),
                null,
                null
        );
    }
}

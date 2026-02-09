package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.payment.service.PaymentWebhookService;
import com.example.study_cards.infra.payment.config.TossPaymentProperties;
import com.example.study_cards.infra.payment.dto.TossWebhookPayload.DataPayload;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class PaymentWebhookControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TossPaymentProperties tossPaymentProperties;

    @MockitoBean
    private PaymentWebhookService paymentWebhookService;

    @BeforeEach
    void setUp() {
        given(tossPaymentProperties.getWebhookSecret()).willReturn(null);
    }

    @Nested
    @DisplayName("POST /api/webhooks/toss")
    class HandleTossWebhookTest {

        @Test
        @DisplayName("PAYMENT_STATUS_CHANGED - DONE 이벤트를 서비스에 위임한다")
        void handleWebhook_paymentDone_delegatesToService() throws Exception {
            String payload = """
                    {
                        "eventType": "PAYMENT_STATUS_CHANGED",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": {
                            "paymentKey": "payment_key_123",
                            "orderId": "ORDER_12345",
                            "status": "DONE",
                            "totalAmount": 3900,
                            "method": "카드",
                            "approvedAt": "2024-01-01T10:00:00"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andDo(document("webhook/toss-payment-done",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("이벤트 생성 시간"),
                                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("이벤트 데이터"),
                                    fieldWithPath("data.paymentKey").type(JsonFieldType.STRING).description("결제 키"),
                                    fieldWithPath("data.orderId").type(JsonFieldType.STRING).description("주문 ID"),
                                    fieldWithPath("data.status").type(JsonFieldType.STRING).description("결제 상태"),
                                    fieldWithPath("data.totalAmount").type(JsonFieldType.NUMBER).description("결제 금액"),
                                    fieldWithPath("data.method").type(JsonFieldType.STRING).description("결제 수단"),
                                    fieldWithPath("data.approvedAt").type(JsonFieldType.STRING).description("승인 시간")
                            )
                    ));

            verify(paymentWebhookService).handlePaymentStatusChanged(
                    argThat((DataPayload data) -> "DONE".equals(data.status()) && "ORDER_12345".equals(data.orderId())));
        }

        @Test
        @DisplayName("PAYMENT_STATUS_CHANGED - CANCELED 이벤트를 서비스에 위임한다")
        void handleWebhook_paymentCanceled_delegatesToService() throws Exception {
            String payload = """
                    {
                        "eventType": "PAYMENT_STATUS_CHANGED",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": {
                            "paymentKey": "payment_key_123",
                            "orderId": "ORDER_12345",
                            "status": "CANCELED",
                            "totalAmount": 3900,
                            "method": "카드",
                            "canceledAt": "2024-01-01T11:00:00",
                            "cancelReason": "고객 요청에 의한 취소"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(paymentWebhookService).handlePaymentStatusChanged(
                    argThat((DataPayload data) -> "CANCELED".equals(data.status())));
        }

        @Test
        @DisplayName("PAYMENT_STATUS_CHANGED - ABORTED 이벤트를 서비스에 위임한다")
        void handleWebhook_paymentAborted_delegatesToService() throws Exception {
            String payload = """
                    {
                        "eventType": "PAYMENT_STATUS_CHANGED",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": {
                            "paymentKey": "payment_key_123",
                            "orderId": "ORDER_12345",
                            "status": "ABORTED",
                            "totalAmount": 3900,
                            "method": "카드"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(paymentWebhookService).handlePaymentStatusChanged(
                    argThat((DataPayload data) -> "ABORTED".equals(data.status())));
        }

        @Test
        @DisplayName("BILLING_KEY_DELETED 이벤트를 서비스에 위임한다")
        void handleWebhook_billingKeyDeleted_delegatesToService() throws Exception {
            String payload = """
                    {
                        "eventType": "BILLING_DELETED",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": {
                            "billingKey": "billing_key_123",
                            "customerKey": "customer_key_123"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andDo(document("webhook/toss-billing-key-deleted",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("이벤트 생성 시간"),
                                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("이벤트 데이터"),
                                    fieldWithPath("data.billingKey").type(JsonFieldType.STRING).description("빌링 키"),
                                    fieldWithPath("data.customerKey").type(JsonFieldType.STRING).description("고객 키")
                            )
                    ));

            verify(paymentWebhookService).handleBillingKeyDeleted(
                    argThat((DataPayload data) -> "billing_key_123".equals(data.billingKey())));
        }

        @Test
        @DisplayName("알 수 없는 이벤트 타입은 무시하고 200을 반환한다")
        void handleWebhook_unknownEventType_returns200() throws Exception {
            String payload = """
                    {
                        "eventType": "UNKNOWN_EVENT",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": {
                            "paymentKey": "payment_key_123",
                            "orderId": "ORDER_12345",
                            "status": "UNKNOWN"
                        }
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(paymentWebhookService, never()).handlePaymentStatusChanged(argThat(data -> true));
            verify(paymentWebhookService, never()).handleBillingKeyDeleted(argThat(data -> true));
        }

        @Test
        @DisplayName("data가 null인 경우 200을 반환한다")
        void handleWebhook_nullData_returns200() throws Exception {
            String payload = """
                    {
                        "eventType": "PAYMENT_STATUS_CHANGED",
                        "createdAt": "2024-01-01T10:00:00",
                        "data": null
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(paymentWebhookService, never()).handlePaymentStatusChanged(argThat(data -> true));
        }
    }

    @Nested
    @DisplayName("서명 검증")
    class SignatureVerificationTest {

        @Test
        @DisplayName("유효한 서명으로 요청하면 성공한다")
        void handleWebhook_validSignature_success() throws Exception {
            String webhookSecret = "test_webhook_secret_key";
            given(tossPaymentProperties.getWebhookSecret()).willReturn(webhookSecret);

            String payload = """
                    {"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2024-01-01T10:00:00","data":{"paymentKey":"payment_key_123","orderId":"ORDER_12345","status":"DONE","totalAmount":3900,"method":"카드","approvedAt":"2024-01-01T10:00:00"}}""";

            String signature = generateSignature(payload, webhookSecret);

            mockMvc.perform(post("/api/webhooks/toss")
                            .header("Toss-Signature", signature)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("잘못된 서명으로 요청하면 실패한다")
        void handleWebhook_invalidSignature_fails() throws Exception {
            String webhookSecret = "test_webhook_secret_key";
            given(tossPaymentProperties.getWebhookSecret()).willReturn(webhookSecret);

            String payload = """
                    {"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2024-01-01T10:00:00","data":{"paymentKey":"payment_key_123","orderId":"ORDER_12345","status":"DONE","totalAmount":3900,"method":"카드","approvedAt":"2024-01-01T10:00:00"}}""";

            mockMvc.perform(post("/api/webhooks/toss")
                            .header("Toss-Signature", "invalid_signature")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("서명 헤더가 없으면 실패한다")
        void handleWebhook_noSignature_fails() throws Exception {
            String webhookSecret = "test_webhook_secret_key";
            given(tossPaymentProperties.getWebhookSecret()).willReturn(webhookSecret);

            String payload = """
                    {"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2024-01-01T10:00:00","data":{"paymentKey":"payment_key_123","orderId":"ORDER_12345","status":"DONE"}}""";

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String generateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}

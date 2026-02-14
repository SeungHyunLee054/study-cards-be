package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.payment.service.PaymentWebhookService;
import com.example.study_cards.infra.payment.dto.response.TossWebhookPayload.DataPayload;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    private PaymentWebhookService paymentWebhookService;

    @Nested
    @DisplayName("POST /api/webhooks/toss")
    class HandleTossWebhookTest {

        @Test
        @DisplayName("PAYMENT_STATUS_CHANGED - DONE 이벤트를 처리한다")
        void handleWebhook_paymentDone_success() throws Exception {
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

            verify(paymentWebhookService).handlePaymentStatusChanged(any(DataPayload.class));
        }

        @Test
        @DisplayName("PAYMENT_STATUS_CHANGED - CANCELED 이벤트를 처리한다")
        void handleWebhook_paymentCanceled_success() throws Exception {
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

            verify(paymentWebhookService).handlePaymentStatusChanged(any(DataPayload.class));
        }

        @Test
        @DisplayName("BILLING_DELETED 이벤트를 처리한다")
        void handleWebhook_billingDeleted_success() throws Exception {
            String payload = """
                    {
                        "eventType": "BILLING_DELETED",
                        "createdAt": "2024-01-01T10:00:00",
                        "billingKey": "billing_key_123",
                        "reason": "user_request"
                    }
                    """;

            mockMvc.perform(post("/api/webhooks/toss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andDo(document("webhook/toss-billing-deleted",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("이벤트 생성 시간"),
                                    fieldWithPath("billingKey").type(JsonFieldType.STRING).description("빌링 키"),
                                    fieldWithPath("reason").type(JsonFieldType.STRING).description("삭제 사유")
                            )
                    ));

            verify(paymentWebhookService).handleBillingKeyDeleted(any(DataPayload.class));
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

            verify(paymentWebhookService, never()).handlePaymentStatusChanged(any());
            verify(paymentWebhookService, never()).handleBillingKeyDeleted(any());
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

            verify(paymentWebhookService, never()).handlePaymentStatusChanged(any());
        }
    }

}

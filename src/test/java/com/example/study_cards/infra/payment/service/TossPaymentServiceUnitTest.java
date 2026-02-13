package com.example.study_cards.infra.payment.service;

import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.infra.payment.dto.response.TossBillingAuthResponse;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;;

@MockitoSettings(strictness = Strictness.LENIENT)
class TossPaymentServiceUnitTest extends BaseUnitTest {

    private RestClient tossPaymentRestClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    private TossPaymentService tossPaymentService;

    @BeforeEach
    void setUp() {
        tossPaymentRestClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        // Setup common mock chain for POST
        given(tossPaymentRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodyUriSpec.uri(anyString(), any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willAnswer(invocation -> requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);

        // Setup common mock chain for GET
        willReturn(requestHeadersUriSpec).given(tossPaymentRestClient).get();
        willReturn(requestHeadersSpec).given(requestHeadersUriSpec).uri(anyString(), any(Object.class));
        willReturn(responseSpec).given(requestHeadersSpec).retrieve();

        tossPaymentService = new TossPaymentService(tossPaymentRestClient);
    }

    @Nested
    @DisplayName("issueBillingKey")
    class IssueBillingKeyTest {

        @Test
        @DisplayName("빌링키 발급 성공 시 응답을 반환한다")
        void issueBillingKey_success() {
            // given
            TossBillingAuthResponse expectedResponse = new TossBillingAuthResponse(
                    "billing_key_123", "customer_key_123", "2024-01-01T10:00:00", "카드", null);
            given(responseSpec.body(TossBillingAuthResponse.class)).willReturn(expectedResponse);

            // when
            TossBillingAuthResponse result = tossPaymentService.issueBillingKey("auth_key", "customer_key_123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.billingKey()).isEqualTo("billing_key_123");
        }

        @Test
        @DisplayName("응답이 null이면 예외를 발생시킨다")
        void issueBillingKey_nullResponse_throwsException() {
            // given
            given(responseSpec.body(TossBillingAuthResponse.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> tossPaymentService.issueBillingKey("auth_key", "customer_key"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED);
                    });
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPaymentTest {

        @Test
        @DisplayName("결제 승인 성공 시 응답을 반환한다")
        void confirmPayment_success() {
            // given
            String paymentKey = "test_payment_key";
            String orderId = "ORDER_12345";
            Integer amount = 3900;

            TossConfirmResponse expectedResponse = createMockResponse(paymentKey, orderId, amount);
            given(responseSpec.body(TossConfirmResponse.class)).willReturn(expectedResponse);

            // when
            TossConfirmResponse result = tossPaymentService.confirmPayment(paymentKey, orderId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(result.paymentKey()).isEqualTo(paymentKey);
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.totalAmount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("응답이 null이면 예외를 발생시킨다")
        void confirmPayment_nullResponse_throwsException() {
            // given
            given(responseSpec.body(TossConfirmResponse.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> tossPaymentService.confirmPayment("key", "order", 1000))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED);
                    });
        }

        @Test
        @DisplayName("예외 발생 시 PaymentException을 발생시킨다")
        void confirmPayment_exception_throwsPaymentException() {
            // given
            given(tossPaymentRestClient.post()).willThrow(new RuntimeException("Connection error"));

            // when & then
            assertThatThrownBy(() -> tossPaymentService.confirmPayment("key", "order", 1000))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED);
                    });
        }
    }

    @Nested
    @DisplayName("billingPayment")
    class BillingPaymentTest {

        @Test
        @DisplayName("빌링 결제 성공 시 응답을 반환한다")
        void billingPayment_success() {
            // given
            String billingKey = "billing_key_123";
            String customerKey = "customer_key_123";
            String orderId = "ORDER_12345";
            Integer amount = 3900;
            String orderName = "프리미엄 구독";

            TossConfirmResponse expectedResponse = createMockResponse("payment_key", orderId, amount);
            given(responseSpec.body(TossConfirmResponse.class)).willReturn(expectedResponse);

            // when
            TossConfirmResponse result = tossPaymentService.billingPayment(
                    billingKey, customerKey, orderId, amount, orderName);

            // then
            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.totalAmount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("응답이 null이면 예외를 발생시킨다")
        void billingPayment_nullResponse_throwsException() {
            // given
            given(responseSpec.body(TossConfirmResponse.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> tossPaymentService.billingPayment(
                    "billing_key", "customer_key", "order", 1000, "orderName"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_PAYMENT_FAILED);
                    });
        }

        @Test
        @DisplayName("예외 발생 시 PaymentException을 발생시킨다")
        void billingPayment_exception_throwsPaymentException() {
            // given
            given(tossPaymentRestClient.post()).willThrow(new RuntimeException("Connection error"));

            // when & then
            assertThatThrownBy(() -> tossPaymentService.billingPayment(
                    "billing_key", "customer_key", "order", 1000, "orderName"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_PAYMENT_FAILED);
                    });
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPaymentTest {

        @Test
        @DisplayName("결제 취소 성공")
        void cancelPayment_success() {
            // given
            String paymentKey = "payment_key_123";
            String cancelReason = "고객 요청에 의한 취소";

            given(responseSpec.toBodilessEntity()).willReturn(null);

            // when & then (no exception)
            tossPaymentService.cancelPayment(paymentKey, cancelReason);
        }

        @Test
        @DisplayName("예외 발생 시 PaymentException을 발생시킨다")
        void cancelPayment_exception_throwsPaymentException() {
            // given
            given(tossPaymentRestClient.post()).willThrow(new RuntimeException("Connection error"));

            // when & then
            assertThatThrownBy(() -> tossPaymentService.cancelPayment("key", "reason"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_CANCEL_FAILED);
                    });
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPaymentTest {

        @Test
        @DisplayName("결제 정보 조회 성공")
        void getPayment_success() {
            // given
            String paymentKey = "payment_key_123";
            TossConfirmResponse expectedResponse = createMockResponse(paymentKey, "ORDER_123", 3900);

            given(responseSpec.body(TossConfirmResponse.class)).willReturn(expectedResponse);

            // when
            TossConfirmResponse result = tossPaymentService.getPayment(paymentKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.paymentKey()).isEqualTo(paymentKey);
        }

        @Test
        @DisplayName("예외 발생 시 PaymentException을 발생시킨다")
        void getPayment_exception_throwsPaymentException() {
            // given
            given(tossPaymentRestClient.get()).willThrow(new RuntimeException("Connection error"));

            // when & then
            assertThatThrownBy(() -> tossPaymentService.getPayment("key"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> {
                        PaymentException pe = (PaymentException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
                    });
        }
    }

    private TossConfirmResponse createMockResponse(String paymentKey, String orderId, Integer amount) {
        return new TossConfirmResponse(
                paymentKey,
                orderId,
                "주문명",
                "DONE",
                amount,
                "카드",
                "2024-01-01T10:00:00",
                "2024-01-01T10:00:00",
                null,
                null
        );
    }
}

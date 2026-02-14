package com.example.study_cards.application.subscription.scheduler;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.common.aop.DistributedLock;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubscriptionRenewalScheduler {

    private final SubscriptionDomainService subscriptionDomainService;
    private final PaymentDomainService paymentDomainService;
    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentService tossPaymentService;
    private final NotificationService notificationService;

    @Value("${app.subscription.grace-period-days:3}")
    private int gracePeriodDays;

    @Scheduled(cron = "${app.subscription.renewal-check-cron:0 0 9 * * *}")
    @Transactional
    @DistributedLock(key = "scheduler:subscription-renewal", ttlMinutes = 30)
    public void checkAndRenewSubscriptions() {
        log.info("Starting subscription renewal check");

        List<Subscription> renewableSubscriptions = subscriptionDomainService.findRenewableSubscriptions(gracePeriodDays);
        log.info("Found {} subscriptions to renew", renewableSubscriptions.size());

        for (Subscription subscription : renewableSubscriptions) {
            try {
                renewSubscription(subscription);
            } catch (Exception e) {
                log.error("Failed to renew subscription: userId={}, error={}",
                        subscription.getUser().getId(), e.getMessage(), e);
                handleRenewalFailure(subscription);
            }
        }

        expireOldSubscriptions();

        log.info("Subscription renewal check completed");
    }

    private void renewSubscription(Subscription subscription) {
        if (subscription.getBillingCycle() != BillingCycle.MONTHLY) {
            log.warn("Skipping non-monthly subscription renewal: subscriptionId={}, billingCycle={}",
                    subscription.getId(), subscription.getBillingCycle());
            return;
        }

        if (subscription.getBillingKey() == null) {
            log.warn("Subscription has no billing key: subscriptionId={}", subscription.getId());
            return;
        }

        int amount = subscription.getPlan().getPrice(subscription.getBillingCycle());
        String orderName = String.format("%s %s 구독 갱신",
                subscription.getPlan().getDisplayName(),
                subscription.getBillingCycle().getDisplayName());

        Payment payment = paymentDomainService.createPayment(
                subscription.getUser(),
                subscription,
                amount,
                PaymentType.RENEWAL
        );

        TossConfirmResponse response;
        try {
            response = tossPaymentService.billingPayment(
                    subscription.getBillingKey(),
                    subscription.getCustomerKey(),
                    payment.getOrderId(),
                    amount,
                    orderName
            );
        } catch (Exception e) {
            paymentDomainService.failPayment(payment, e.getMessage());
            throw e;
        }

        try {
            paymentDomainService.completePayment(
                    payment,
                    response.paymentKey(),
                    response.method()
            );

            subscriptionDomainService.renewSubscription(subscription);

            log.info("Subscription renewed successfully: userId={}, plan={}, nextEndDate={}",
                    subscription.getUser().getId(),
                    subscription.getPlan(),
                    subscription.getEndDate());
        } catch (Exception e) {
            log.error("결제는 성공했으나 DB 처리 실패. 수동 확인 필요: userId={}, paymentKey={}, orderId={}",
                    subscription.getUser().getId(), response.paymentKey(), payment.getOrderId(), e);
            throw e;
        }
    }

    private void handleRenewalFailure(Subscription subscription) {
        log.warn("Renewal failed for subscription: userId={}", subscription.getUser().getId());

        notificationService.sendNotification(
                subscription.getUser(),
                NotificationType.PAYMENT_FAILED,
                "결제 실패",
                "구독 갱신 결제가 실패했습니다. 결제 수단을 확인해주세요."
        );
    }

    private void expireOldSubscriptions() {
        List<Subscription> expiredSubscriptions = subscriptionDomainService.findExpiredSubscriptions();

        for (Subscription subscription : expiredSubscriptions) {
            try {
                subscriptionDomainService.expireSubscription(subscription);
                log.info("Subscription expired: userId={}", subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Failed to expire subscription: userId={}, error={}",
                        subscription.getUser().getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.notification.expiry-check-cron:0 0 9 * * *}")
    @Transactional
    @DistributedLock(key = "scheduler:expiry-notification", ttlMinutes = 30)
    public void checkExpiringSubscriptions() {
        log.info("Starting subscription expiry notification check");

        LocalDate today = LocalDate.now();

        checkAndNotifyExpiring(today.plusDays(7), NotificationType.SUBSCRIPTION_EXPIRING_7, "7일");
        checkAndNotifyExpiring(today.plusDays(3), NotificationType.SUBSCRIPTION_EXPIRING_3, "3일");
        checkAndNotifyExpiring(today.plusDays(1), NotificationType.SUBSCRIPTION_EXPIRING_1, "1일");

        log.info("Subscription expiry notification check completed");
    }

    private void checkAndNotifyExpiring(LocalDate targetDate, NotificationType type, String daysText) {
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<Subscription> expiringSubscriptions = subscriptionRepository.findExpiringOn(startOfDay, endOfDay);
        log.info("Found {} subscriptions expiring on {} (D-{})", expiringSubscriptions.size(), targetDate, daysText);

        for (Subscription subscription : expiringSubscriptions) {
            if (!notificationService.existsNotification(subscription.getUser(), type, subscription.getId())) {
                notificationService.sendNotification(
                        subscription.getUser(),
                        type,
                        "구독 만료 임박",
                        "구독이 " + daysText + " 후 만료됩니다. 계속 이용하시려면 갱신해주세요.",
                        subscription.getId()
                );
            }
        }
    }
}

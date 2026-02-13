package com.example.study_cards.application.generation.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.study_cards.application.generation.dto.request.ApprovalRequest;
import com.example.study_cards.application.generation.dto.response.GeneratedCardResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.service.GeneratedCardDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class GenerationApprovalService {

    private final GeneratedCardDomainService generatedCardDomainService;
    private final CardDomainService cardDomainService;

    public Page<GeneratedCardResponse> getGeneratedCards(GenerationStatus status, String model, Pageable pageable) {
        Page<GeneratedCard> cards;

        if (status != null && model != null && !model.isBlank()) {
            cards = generatedCardDomainService.findByStatusAndModel(status, model, pageable);
        } else if (status != null) {
            cards = generatedCardDomainService.findByStatus(status, pageable);
        } else if (model != null && !model.isBlank()) {
            cards = generatedCardDomainService.findByModel(model, pageable);
        } else {
            cards = generatedCardDomainService.findAll(pageable);
        }

        return cards.map(GeneratedCardResponse::from);
    }

    public GeneratedCardResponse getGeneratedCard(Long id) {
        return GeneratedCardResponse.from(generatedCardDomainService.findById(id));
    }

    @Transactional
    public GeneratedCardResponse approve(Long id) {
        GeneratedCard approvedCard = generatedCardDomainService.approve(id);
        log.info("생성된 카드 승인 완료 - id: {}", id);
        return GeneratedCardResponse.from(approvedCard);
    }

    @Transactional
    public GeneratedCardResponse reject(Long id) {
        GeneratedCard rejectedCard = generatedCardDomainService.reject(id);
        log.info("생성된 카드 거부 완료 - id: {}", id);
        return GeneratedCardResponse.from(rejectedCard);
    }

    @Transactional
    public List<GeneratedCardResponse> batchApprove(ApprovalRequest request) {
        List<GeneratedCardResponse> approved = request.ids().stream()
                .map(generatedCardDomainService::approve)
                .map(GeneratedCardResponse::from)
                .toList();

        log.info("생성된 카드 일괄 승인 완료 - count: {}", approved.size());
        return approved;
    }

    private static final int MIGRATION_BATCH_SIZE = 100;

    @Transactional
    public int migrateApprovedToCards() {
        int migratedCount = 0;
        Page<GeneratedCard> page;

        do {
            page = generatedCardDomainService.findApprovedCards(PageRequest.of(0, MIGRATION_BATCH_SIZE));

            if (page.isEmpty()) {
                if (migratedCount == 0) {
                    log.info("이동할 승인된 카드가 없습니다.");
                }
                break;
            }

            for (GeneratedCard generatedCard : page.getContent()) {
                Card card = generatedCard.toCard();
                cardDomainService.createCard(
                        card.getQuestion(),
                        card.getQuestionSub(),
                        card.getAnswer(),
                        card.getAnswerSub(),
                        card.getCategory(),
                        true
                );
                generatedCardDomainService.markAsMigrated(generatedCard);
                migratedCount++;
            }
        } while (!page.isEmpty());

        if (migratedCount > 0) {
            log.info("승인된 카드 Card 테이블로 이동 완료 - count: {}", migratedCount);
        }
        return migratedCount;
    }
}

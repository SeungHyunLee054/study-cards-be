package com.example.study_cards.domain.generation.service;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.domain.generation.repository.GeneratedCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GeneratedCardDomainService {

    private final GeneratedCardRepository generatedCardRepository;

    public GeneratedCard save(GeneratedCard generatedCard) {
        return generatedCardRepository.save(generatedCard);
    }

    public List<GeneratedCard> saveAll(List<GeneratedCard> generatedCards) {
        return generatedCardRepository.saveAll(generatedCards);
    }

    public GeneratedCard findById(Long id) {
        return generatedCardRepository.findById(id)
                .orElseThrow(() -> new GenerationException(GenerationErrorCode.GENERATED_CARD_NOT_FOUND));
    }

    public Page<GeneratedCard> findByStatus(GenerationStatus status, Pageable pageable) {
        return generatedCardRepository.findByStatus(status, pageable);
    }

    public Page<GeneratedCard> findByStatusAndModel(GenerationStatus status, String model, Pageable pageable) {
        return generatedCardRepository.findByStatusAndModel(status, model, pageable);
    }

    public Page<GeneratedCard> findByStatusAndCategory(GenerationStatus status, Category category, Pageable pageable) {
        return generatedCardRepository.findByStatusAndCategory(status, category, pageable);
    }

    public Page<GeneratedCard> findByModel(String model, Pageable pageable) {
        return generatedCardRepository.findByModel(model, pageable);
    }

    public Page<GeneratedCard> findAll(Pageable pageable) {
        return generatedCardRepository.findAll(pageable);
    }

    public List<GeneratedCard> findApprovedCards() {
        return generatedCardRepository.findByStatusWithCategory(GenerationStatus.APPROVED);
    }

    public Page<GeneratedCard> findApprovedCards(Pageable pageable) {
        return generatedCardRepository.findByStatusWithCategory(GenerationStatus.APPROVED, pageable);
    }

    public GeneratedCard approve(Long id) {
        GeneratedCard generatedCard = findById(id);
        validateStatusForApproval(generatedCard);
        generatedCard.approve();
        return generatedCard;
    }

    public GeneratedCard reject(Long id) {
        GeneratedCard generatedCard = findById(id);
        validateStatusForRejection(generatedCard);
        generatedCard.reject();
        return generatedCard;
    }

    public void markAsMigrated(GeneratedCard generatedCard) {
        generatedCard.markAsMigrated();
    }

    public long countByStatus(GenerationStatus status) {
        return generatedCardRepository.countByStatus(status);
    }

    public long countByModel(String model) {
        return generatedCardRepository.countByModel(model);
    }

    public long countByModelAndStatus(String model, GenerationStatus status) {
        return generatedCardRepository.countByModelAndStatus(model, status);
    }

    public List<Object[]> countByModelGroupByStatus() {
        return generatedCardRepository.countByModelGroupByStatus();
    }

    public long count() {
        return generatedCardRepository.count();
    }

    private void validateStatusForApproval(GeneratedCard generatedCard) {
        if (generatedCard.isApproved()) {
            throw new GenerationException(GenerationErrorCode.ALREADY_APPROVED);
        }
        if (generatedCard.getStatus() == GenerationStatus.MIGRATED) {
            throw new GenerationException(GenerationErrorCode.ALREADY_MIGRATED);
        }
    }

    private void validateStatusForRejection(GeneratedCard generatedCard) {
        if (generatedCard.getStatus() == GenerationStatus.REJECTED) {
            throw new GenerationException(GenerationErrorCode.ALREADY_REJECTED);
        }
        if (generatedCard.getStatus() == GenerationStatus.MIGRATED) {
            throw new GenerationException(GenerationErrorCode.ALREADY_MIGRATED);
        }
    }
}

package com.example.study_cards.domain.card.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.card.repository.CardRepositoryCustom.CategoryCount;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CardDomainService {

    private final CardRepository cardRepository;
    private final StudyRecordRepository studyRecordRepository;

    public Card createCard(String question, String questionSub, String answer, String answerSub, Category category, boolean aiGenerated) {
        Card card = Card.builder()
                .question(question)
                .questionSub(questionSub)
                .answer(answer)
                .answerSub(answerSub)
                .category(category)
                .aiGenerated(aiGenerated)
                .build();
        return cardRepository.save(card);
    }

    public Card findById(Long id) {
        return cardRepository.findByIdAndStatus(id, CardStatus.ACTIVE)
                .orElseThrow(() -> new CardException(CardErrorCode.CARD_NOT_FOUND));
    }

    public List<Card> findAll() {
        return cardRepository.findByStatus(CardStatus.ACTIVE);
    }

    public List<Card> findByCategory(Category category) {
        return cardRepository.findByCategoryAndStatus(category, CardStatus.ACTIVE);
    }

    public List<Card> findByIdsInCategory(List<Long> ids, Category category) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return cardRepository.findByIdInAndCategoryAndStatus(ids, category, CardStatus.ACTIVE);
    }

    public List<Card> findCardsForStudy() {
        return cardRepository.findAllByOrderByEfFactorAsc();
    }

    public List<Card> findCardsForStudyByCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return cardRepository.findByCategoriesOrderByEfFactorAsc(categories);
    }

    public Card updateCard(Long id, String question, String questionSub, String answer, String answerSub, Category category) {
        Card card = findById(id);
        card.update(question, questionSub, answer, answerSub, category);
        return card;
    }

    public void deleteCard(Long id) {
        Card card = findById(id);
        if (studyRecordRepository.existsByCard(card)) {
            throw new CardException(CardErrorCode.CARD_HAS_STUDY_RECORDS);
        }
        card.delete();
    }

    public long count() {
        return cardRepository.countByStatus(CardStatus.ACTIVE);
    }

    public long countByCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return 0L;
        }
        return cardRepository.countByCategoryInAndStatus(categories, CardStatus.ACTIVE);
    }

    public Page<Card> findAll(Pageable pageable) {
        return cardRepository.findAllWithCategory(pageable);
    }

    public Page<Card> findByCategories(List<Category> categories, Pageable pageable) {
        return cardRepository.findByCategoriesWithCategory(categories, pageable);
    }

    public Page<Card> findCardsForStudy(Pageable pageable) {
        return cardRepository.findAllByOrderByEfFactorAscWithCategory(pageable);
    }

    public Page<Card> findCardsForStudyByCategories(List<Category> categories, Pageable pageable) {
        return cardRepository.findByCategoriesOrderByEfFactorAscWithCategory(categories, pageable);
    }

    public Page<Card> searchByKeyword(String keyword, List<Category> categories, Pageable pageable) {
        return cardRepository.searchByKeyword(keyword, categories, pageable);
    }

    public List<CategoryCount> countAllByCategory() {
        return cardRepository.countByCategory();
    }
}

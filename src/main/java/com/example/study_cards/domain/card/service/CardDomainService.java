package com.example.study_cards.domain.card.service;

import com.example.study_cards.domain.card.entity.Card;
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

    public Card createCard(String question, String questionSub, String answer, String answerSub, Category category) {
        Card card = Card.builder()
                .question(question)
                .questionSub(questionSub)
                .answer(answer)
                .answerSub(answerSub)
                .category(category)
                .build();
        return cardRepository.save(card);
    }

    public Card findById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardException(CardErrorCode.CARD_NOT_FOUND));
    }

    public List<Card> findAll() {
        return cardRepository.findAll();
    }

    public List<Card> findByCategory(Category category) {
        return cardRepository.findByCategory(category);
    }

    public List<Card> findCardsForStudy() {
        return cardRepository.findAllByOrderByEfFactorAsc();
    }

    public List<Card> findCardsForStudyByCategory(Category category) {
        return cardRepository.findByCategoryOrderByEfFactorAsc(category);
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
        cardRepository.delete(card);
    }

    public long count() {
        return cardRepository.count();
    }

    public long countByCategory(Category category) {
        return cardRepository.countByCategory(category);
    }

    public Page<Card> findAll(Pageable pageable) {
        return cardRepository.findAllWithCategory(pageable);
    }

    public Page<Card> findByCategory(Category category, Pageable pageable) {
        return cardRepository.findByCategoryWithCategory(category, pageable);
    }

    public Page<Card> findCardsForStudy(Pageable pageable) {
        return cardRepository.findAllByOrderByEfFactorAscWithCategory(pageable);
    }

    public Page<Card> findCardsForStudyByCategory(Category category, Pageable pageable) {
        return cardRepository.findByCategoryOrderByEfFactorAscWithCategory(category, pageable);
    }

    public List<CategoryCount> countAllByCategory() {
        return cardRepository.countByCategory();
    }
}

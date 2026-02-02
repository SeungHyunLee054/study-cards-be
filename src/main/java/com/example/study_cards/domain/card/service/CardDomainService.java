package com.example.study_cards.domain.card.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CardDomainService {

    private final CardRepository cardRepository;

    public Card createCard(String questionEn, String questionKo, String answerEn, String answerKo, Category category) {
        Card card = Card.builder()
                .questionEn(questionEn)
                .questionKo(questionKo)
                .answerEn(answerEn)
                .answerKo(answerKo)
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

    public Card updateCard(Long id, String questionEn, String questionKo, String answerEn, String answerKo, Category category) {
        Card card = findById(id);
        card.update(questionEn, questionKo, answerEn, answerKo, category);
        return card;
    }

    public void deleteCard(Long id) {
        Card card = findById(id);
        cardRepository.delete(card);
    }

    public long count() {
        return cardRepository.count();
    }

    public long countByCategory(Category category) {
        return cardRepository.countByCategory(category);
    }
}

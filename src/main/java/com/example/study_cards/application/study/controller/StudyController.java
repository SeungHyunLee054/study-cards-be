package com.example.study_cards.application.study.controller;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.application.study.service.StudyService;
import com.example.study_cards.domain.card.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/cards")
    public ResponseEntity<List<StudyCardResponse>> getTodayCards(
            @RequestParam(required = false, defaultValue = "CS") Category category) {
        // TODO
        return null;
    }

    @PostMapping("/answer")
    public ResponseEntity<StudyResultResponse> submitAnswer(@RequestBody StudyAnswerRequest request) {
        // TODO
        return null;
    }
}

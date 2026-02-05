package com.example.study_cards.application.generation.controller;

import com.example.study_cards.application.generation.dto.request.ApprovalRequest;
import com.example.study_cards.application.generation.dto.request.GenerationRequest;
import com.example.study_cards.application.generation.dto.response.GeneratedCardResponse;
import com.example.study_cards.application.generation.dto.response.GenerationResultResponse;
import com.example.study_cards.application.generation.dto.response.GenerationStatsResponse;
import com.example.study_cards.application.generation.service.GenerationApprovalService;
import com.example.study_cards.application.generation.service.GenerationService;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/generation")
public class AdminGenerationController {

    private final GenerationService generationService;
    private final GenerationApprovalService approvalService;

    @PostMapping("/cards")
    public ResponseEntity<GenerationResultResponse> generateCards(@Valid @RequestBody GenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(generationService.generateCards(request));
    }

    @GetMapping("/stats")
    public ResponseEntity<GenerationStatsResponse> getStats() {
        return ResponseEntity.ok(generationService.getStats());
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<GeneratedCardResponse>> getGeneratedCards(
            @RequestParam(required = false) GenerationStatus status,
            @RequestParam(required = false) String model,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(approvalService.getGeneratedCards(status, model, pageable));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<GeneratedCardResponse> getGeneratedCard(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.getGeneratedCard(id));
    }

    @PatchMapping("/cards/{id}/approve")
    public ResponseEntity<GeneratedCardResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.approve(id));
    }

    @PatchMapping("/cards/{id}/reject")
    public ResponseEntity<GeneratedCardResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.reject(id));
    }

    @PostMapping("/cards/batch-approve")
    public ResponseEntity<List<GeneratedCardResponse>> batchApprove(@Valid @RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(approvalService.batchApprove(request));
    }

    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrateApprovedCards() {
        int migratedCount = approvalService.migrateApprovedToCards();
        return ResponseEntity.ok(Map.of(
                "migratedCount", migratedCount,
                "message", "승인된 카드 " + migratedCount + "개를 Card로 이동 완료"
        ));
    }
}

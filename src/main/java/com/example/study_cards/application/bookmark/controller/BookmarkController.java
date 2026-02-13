package com.example.study_cards.application.bookmark.controller;

import com.example.study_cards.application.bookmark.dto.response.BookmarkResponse;
import com.example.study_cards.application.bookmark.dto.response.BookmarkStatusResponse;
import com.example.study_cards.application.bookmark.service.BookmarkService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/cards/{cardId}")
    public ResponseEntity<BookmarkResponse> bookmarkCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cardId) {
        return ResponseEntity.ok(bookmarkService.bookmarkCard(userDetails.userId(), cardId));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> unbookmarkCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cardId) {
        bookmarkService.unbookmarkCard(userDetails.userId(), cardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user-cards/{userCardId}")
    public ResponseEntity<BookmarkResponse> bookmarkUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userCardId) {
        return ResponseEntity.ok(bookmarkService.bookmarkUserCard(userDetails.userId(), userCardId));
    }

    @DeleteMapping("/user-cards/{userCardId}")
    public ResponseEntity<Void> unbookmarkUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userCardId) {
        bookmarkService.unbookmarkUserCard(userDetails.userId(), userCardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<BookmarkResponse>> getBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookmarkService.getBookmarks(userDetails.userId(), category, pageable));
    }

    @GetMapping("/cards/{cardId}/status")
    public ResponseEntity<BookmarkStatusResponse> getCardBookmarkStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cardId) {
        return ResponseEntity.ok(bookmarkService.getCardBookmarkStatus(userDetails.userId(), cardId));
    }

    @GetMapping("/user-cards/{userCardId}/status")
    public ResponseEntity<BookmarkStatusResponse> getUserCardBookmarkStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userCardId) {
        return ResponseEntity.ok(bookmarkService.getUserCardBookmarkStatus(userDetails.userId(), userCardId));
    }
}

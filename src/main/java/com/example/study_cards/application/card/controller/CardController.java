package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.card.service.CardService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @GetMapping("/study")
    public ResponseEntity<Page<CardResponse>> getCardsForStudy(
            @RequestParam(required = false) String category,
            Authentication authentication,
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "efFactor", direction = Sort.Direction.ASC) Pageable pageable) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String ipAddress = getClientIpAddress(request);
        return ResponseEntity.ok(cardService.getCardsForStudy(category, isAuthenticated, ipAddress, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<CardResponse>> getAllCardsWithUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCardsWithUserCards(userDetails.userId(), category, pageable));
    }

    @GetMapping("/study/all")
    public ResponseEntity<Page<CardResponse>> getCardsForStudyWithUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "efFactor", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsForStudyWithUserCards(userDetails.userId(), category, pageable));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCardCount(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(cardService.getCardCount(category));
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private static final String IP_PATTERN = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$";

    private String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                String clientIp = ip.split(",")[0].trim();
                if (isValidIpAddress(clientIp)) {
                    return clientIp;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.matches(IP_PATTERN);
    }
}

package com.example.study_cards.application.user.controller;

import com.example.study_cards.application.user.dto.response.AdminUserResponse;
import com.example.study_cards.application.user.service.AdminUserService;
import com.example.study_cards.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminUserService.getUsers(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        adminUserService.banUser(id);
        return ResponseEntity.noContent().build();
    }
}

package com.example.study_cards.application.auth.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.SignInResponse;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@RequestBody SignUpRequest request) {
        // TODO
        return null;
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
        // TODO
        return null;
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(@RequestHeader("Authorization") String authorization) {
        // TODO
        return null;
    }
}
